<?php

declare(strict_types=1);

namespace SportsDb\Tests;

use SportsDb\Clock;
use SportsDb\Http\Response;
use SportsDb\Http\Transport;
use SportsDb\Http\TransportException;
use SportsDb\SportsDb;

const PREMIUM = '9999999999'; // a stand-in; fixtures never contain a real key

function fixture(string $path): string
{
    $text = file_get_contents(__DIR__ . "/fixtures/$path");
    if ($text === false) {
        throw new \RuntimeException("missing fixture $path");
    }
    return $text;
}

/** @return list<array<string, mixed>> */
function fixtureRecords(string $path, string $key): array
{
    return json_decode(fixture($path), true)[$key];
}

/** Records requests and answers from a queue, then with $default. */
final class FakeTransport implements Transport
{
    /** @var list<\Closure(): Response> */
    public array $queue = [];
    /** @var list<array{string, array<string, string>}> */
    public array $requests = [];
    /** @var \Closure(): Response */
    public \Closure $default;

    public function __construct(?\Closure $default = null)
    {
        $this->default = $default ?? static fn () => new Response(200, '{}');
    }

    /** @param array<string, string> $headers */
    public function respond(string $body, int $status = 200, array $headers = []): self
    {
        $this->queue[] = static fn () => new Response($status, $body, $headers);
        return $this;
    }

    public function fail(string $message = 'connection reset'): self
    {
        $this->queue[] = static fn () => throw new TransportException($message);
        return $this;
    }

    public function lastUrl(): string
    {
        return $this->requests[array_key_last($this->requests)][0];
    }

    public function get(string $url, array $headers): Response
    {
        $this->requests[] = [$url, $headers];
        $next = array_shift($this->queue) ?? $this->default;
        return $next();
    }
}

/** Answers by URL: the first route whose key is in the call (after /json/) wins; a fixture path or inline JSON. */
final class Routes implements Transport
{
    /** @var list<string> */
    public array $calls = [];

    /** @param array<string, string> $routes */
    public function __construct(private readonly array $routes = [])
    {
    }

    public function get(string $url, array $headers): Response
    {
        $call = explode('/json/', $url, 2)[1];
        $this->calls[] = $call;
        foreach ($this->routes as $key => $body) {
            if (str_contains($call, $key)) {
                return new Response(200, str_starts_with($body, '{') ? $body : fixture($body));
            }
        }
        return new Response(200, '{}');
    }
}

/** A virtual clock: sleep() advances time instead of waiting. */
final class FakeClock implements Clock
{
    /** @var list<float> */
    public array $sleeps = [];
    private float $now = 1_000_000.0;

    public function now(): float
    {
        return $this->now;
    }

    public function sleep(float $seconds): void
    {
        $this->sleeps[] = $seconds;
        $this->now += $seconds;
    }
}

function client(Transport $transport, string $key = '123', mixed ...$options): SportsDb
{
    $options['requestsPerMinute'] ??= 0;
    $options['clock'] ??= new FakeClock();
    $options['transport'] = $transport;
    return new SportsDb($key, ...$options);
}
