<?php

declare(strict_types=1);

namespace SportsDb\Internal;

use SportsDb\Cache\Freshness;
use SportsDb\Clock;
use SportsDb\Config;
use SportsDb\Exception\HttpStatusException;
use SportsDb\Exception\InvalidApiKeyException;
use SportsDb\Exception\NetworkException;
use SportsDb\Exception\PremiumRequiredException;
use SportsDb\Exception\RateLimitException;
use SportsDb\Http\Transport;
use SportsDb\Http\TransportException;
use SportsDb\RequestEvent;

/**
 * Builds URLs and runs every call through the rate limiter, retries, cache and parser.
 *
 * @internal
 */
final class Requester
{
    private ?RateLimiter $limiter;
    private string $tier;

    public function __construct(public readonly Config $config, private readonly Transport $transport, private readonly Clock $clock)
    {
        $rpm = $config->effectiveRequestsPerMinute();
        $this->limiter = $rpm > 0 ? new RateLimiter($rpm, $clock) : null;
        $this->tier = $config->isFreeKey() ? 'free' : 'paid';
    }

    /**
     * GET /api/v1/json/{key}/{endpoint}?params. Null params are left out.
     *
     * @template T
     * @param callable(array<string, string|null>): T $parse
     * @param array<string, scalar|\DateTimeInterface|null> $params
     * @return list<T>
     */
    public function v1(string $endpoint, string $recordKey, Freshness $freshness, callable $parse, array $params = []): array
    {
        $query = [];
        foreach ($params as $k => $v) {
            if ($v !== null) {
                $query[] = $k . '=' . rawurlencode(self::text($v));
            }
        }
        $suffix = $query === [] ? $endpoint : $endpoint . '?' . implode('&', $query);
        $base = rtrim($this->config->baseUrl, '/') . '/api/v1/json/';
        $url = $base . rawurlencode($this->config->apiKey) . '/' . $suffix;
        $display = $base . '***/' . $suffix;
        $body = $this->fetch($url, [], $display, "v1:{$this->tier}:$suffix", $freshness);
        return array_map($parse, Envelope::records($body, $recordKey, $display));
    }

    /**
     * GET /api/v2/json/{segments...} with the key in the X-API-KEY header.
     *
     * @template T
     * @param callable(array<string, string|null>): T $parse
     * @param list<scalar|\DateTimeInterface> $segments
     * @return list<T>
     */
    public function v2(string $recordKey, Freshness $freshness, callable $parse, array $segments): array
    {
        if ($this->config->isFreeKey()) {
            throw new PremiumRequiredException('v2 endpoints need a premium key; free keys (123, 3) only work with v1 ($db->v1).');
        }
        $path = implode('/', array_map(static fn ($s) => rawurlencode(self::text($s)), $segments));
        $url = rtrim($this->config->baseUrl, '/') . '/api/v2/json/' . $path;
        $body = $this->fetch($url, ['X-API-KEY' => $this->config->apiKey], $url, "v2:$path", $freshness);
        return array_map($parse, Envelope::records($body, $recordKey, $url));
    }

    /** @param array<string, string> $headers */
    private function fetch(string $url, array $headers, string $display, string $cacheKey, Freshness $freshness): string
    {
        $started = $this->clock->now();
        $trace = ['status' => null, 'attempts' => 0, 'fromCache' => false];
        $error = null;
        try {
            $cache = $this->config->cache;
            $ttl = $this->config->cachePolicy->ttl($freshness);
            if ($cache !== null && $ttl > 0 && ($cached = $cache->get($cacheKey)) !== null) {
                $trace['fromCache'] = true;
                return $cached;
            }
            $body = $this->send($url, $headers + ['User-Agent' => $this->config->userAgent], $display, $trace);
            // Don't cache an empty body: for v1 it can mean a transient problem, not "no results".
            if ($cache !== null && $ttl > 0 && trim($body) !== '') {
                $cache->put($cacheKey, $body, $ttl);
            }
            return $body;
        } catch (\Throwable $e) {
            $error = $e;
            throw $e;
        } finally {
            $this->notify($display, $trace, $this->clock->now() - $started, $error);
        }
    }

    /**
     * @param array<string, string> $headers
     * @param array{status: int|null, attempts: int, fromCache: bool} $trace
     */
    private function send(string $url, array $headers, string $display, array &$trace): string
    {
        $attempt = 0;
        $rateLimitRetried = false;
        $backoff = $this->config->retryBackoff;
        while (true) {
            $this->limiter?->acquire();
            $trace['attempts']++;
            try {
                $response = $this->transport->get($url, $headers);
            } catch (TransportException $e) {
                if ($attempt++ < $this->config->maxRetries) {
                    $this->clock->sleep($backoff);
                    $backoff *= 2;
                    continue;
                }
                throw new NetworkException("Request to $display failed: {$e->getMessage()}", 0, $e);
            }

            $status = $trace['status'] = $response->status;
            if ($status >= 200 && $status < 300) {
                return $response->body;
            }
            if ($status === 429) {
                $retryAfter = $response->header('Retry-After');
                $wait = $retryAfter !== null && is_numeric(trim($retryAfter)) ? (float) trim($retryAfter) : null;
                if ($this->config->retryOnRateLimit && !$rateLimitRetried) {
                    $rateLimitRetried = true;
                    $this->clock->sleep($wait ?? $this->config->rateLimitWait);
                    continue;
                }
                throw new RateLimitException("Rate limit exceeded calling $display", $wait);
            }
            if ($status >= 500 && $status < 600 && $attempt++ < $this->config->maxRetries) {
                $this->clock->sleep($backoff);
                $backoff *= 2;
                continue;
            }
            if (stripos($response->body, 'invalid premium api key') !== false) {
                throw new InvalidApiKeyException("TheSportsDB rejected the API key (HTTP $status, $display). "
                    . 'Only the free keys (123, 3) and paid keys work; v2 needs a paid key.');
            }
            throw new HttpStatusException("HTTP $status from $display", $status, substr($response->body, 0, 300));
        }
    }

    /** @param array{status: int|null, attempts: int, fromCache: bool} $trace */
    private function notify(string $display, array $trace, float $elapsed, ?\Throwable $error): void
    {
        $listener = $this->config->requestListener;
        if ($listener === null) {
            return;
        }
        $status = $trace['status'] ?? match (true) {
            $error instanceof HttpStatusException => $error->status,
            $error instanceof RateLimitException => 429,
            default => null,
        };
        try {
            $listener(new RequestEvent($display, $status, $trace['attempts'], $trace['fromCache'], $elapsed, $error));
        } catch (\Throwable) {
            // A failing listener must not break the call it is observing.
        }
    }

    private static function text(mixed $value): string
    {
        return match (true) {
            $value instanceof \DateTimeInterface => $value->format('Y-m-d'),
            \is_bool($value) => $value ? '1' : '0',
            default => (string) $value,
        };
    }
}
