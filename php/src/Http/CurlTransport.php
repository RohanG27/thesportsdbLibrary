<?php

declare(strict_types=1);

namespace SportsDb\Http;

/** The default transport, on ext-curl. */
final class CurlTransport implements Transport
{
    public function __construct(private readonly float $timeout = 30.0)
    {
        if (!\function_exists('curl_init')) {
            throw new \LogicException('ext-curl is required for CurlTransport; install it or pass your own Transport.');
        }
    }

    public function get(string $url, array $headers): Response
    {
        $responseHeaders = [];
        $handle = curl_init($url);
        curl_setopt_array($handle, [
            \CURLOPT_RETURNTRANSFER => true,
            \CURLOPT_FOLLOWLOCATION => true,
            \CURLOPT_TIMEOUT_MS => (int) round($this->timeout * 1000),
            \CURLOPT_CONNECTTIMEOUT_MS => (int) round($this->timeout * 1000),
            \CURLOPT_HTTPHEADER => array_map(static fn ($k, $v) => "$k: $v", array_keys($headers), $headers),
            \CURLOPT_HEADERFUNCTION => static function ($_, string $line) use (&$responseHeaders): int {
                $parts = explode(':', $line, 2);
                if (\count($parts) === 2) {
                    $responseHeaders[trim($parts[0])] = trim($parts[1]);
                }
                return \strlen($line);
            },
        ]);
        $body = curl_exec($handle);
        if ($body === false || !\is_string($body)) {
            $error = curl_error($handle) ?: 'request failed';
            throw new TransportException($error);
        }
        $status = (int) curl_getinfo($handle, \CURLINFO_RESPONSE_CODE);
        return new Response($status, $body, $responseHeaders);
    }
}
