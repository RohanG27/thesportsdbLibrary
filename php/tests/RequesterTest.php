<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Tests;

use PHPUnit\Framework\TestCase;
use RohanG27\TheSportsDb\Cache\InMemoryResponseCache;
use RohanG27\TheSportsDb\Exception\HttpStatusException;
use RohanG27\TheSportsDb\Exception\InvalidApiKeyException;
use RohanG27\TheSportsDb\Exception\NetworkException;
use RohanG27\TheSportsDb\Exception\RateLimitException;
use RohanG27\TheSportsDb\Http\Response;
use RohanG27\TheSportsDb\RequestEvent;
use RohanG27\TheSportsDb\SportsDb;

/** Retries, rate limits, errors, redaction, caching, the request listener and timeouts. */
final class RequesterTest extends TestCase
{
    private const PAID = '5550001234';
    private const OK = '{"sports":[{"idSport":"102","strSport":"Soccer"}]}';

    public function testRetriesServerErrorsThenSucceeds(): void
    {
        $clock = new FakeClock();
        $t = (new FakeTransport())->respond('oops', 503)->respond('oops', 502)->respond(self::OK);
        self::assertCount(1, client($t, clock: $clock)->v1->list->sports());
        self::assertCount(3, $t->requests);
        self::assertSame([0.5, 1.0], $clock->sleeps);
    }

    public function testGivesUpAfterMaxRetries(): void
    {
        $t = new FakeTransport(static fn () => new Response(500, 'down'));
        try {
            client($t)->v1->list->sports();
            self::fail('expected HttpStatusException');
        } catch (HttpStatusException $e) {
            self::assertSame(500, $e->status);
        }
        self::assertCount(3, $t->requests);
    }

    public function testNetworkErrorsAreRetriedAndWrapped(): void
    {
        self::assertCount(1, client((new FakeTransport())->fail()->respond(self::OK))->v1->list->sports());
        $this->expectException(NetworkException::class);
        client((new FakeTransport())->fail()->fail()->fail())->v1->list->sports();
    }

    public function testRateLimitWaitsForRetryAfterThenRetriesOnce(): void
    {
        $clock = new FakeClock();
        $t = (new FakeTransport())->respond('', 429, ['Retry-After' => '7'])->respond(self::OK);
        client($t, clock: $clock)->v1->list->sports();
        self::assertSame([7.0], $clock->sleeps);
        $always = new FakeTransport(static fn () => new Response(429, ''));
        try {
            client($always)->v1->list->sports();
            self::fail('expected RateLimitException');
        } catch (RateLimitException $e) {
            self::assertNull($e->retryAfter);
        }
        self::assertCount(2, $always->requests);
    }

    public function testRateLimitRetryCanBeDisabled(): void
    {
        $t = (new FakeTransport())->respond('', 429, ['Retry-After' => '30']);
        try {
            client($t, retryOnRateLimit: false)->v1->list->sports();
            self::fail('expected RateLimitException');
        } catch (RateLimitException $e) {
            self::assertSame(30.0, $e->retryAfter);
        }
        self::assertCount(1, $t->requests);
    }

    public function testClientSideRateLimit(): void
    {
        $clock = new FakeClock();
        $c = new SportsDb(transport: new FakeTransport(static fn () => new Response(200, self::OK)), requestsPerMinute: 3, clock: $clock);
        for ($i = 0; $i < 4; $i++) {
            $c->v1->list->sports();
        }
        self::assertSame([60.0], $clock->sleeps);
    }

    public function testDefaultRateLimitFollowsTheKey(): void
    {
        self::assertSame(30, (new SportsDb('123'))->config->effectiveRequestsPerMinute());
        self::assertSame(30, (new SportsDb('3'))->config->effectiveRequestsPerMinute());
        self::assertSame(100, (new SportsDb(self::PAID))->config->effectiveRequestsPerMinute());
    }

    public function testV1KeyNeverAppearsInErrors(): void
    {
        foreach ([
            [new FakeTransport(static fn () => new Response(404, 'nope')), HttpStatusException::class],
            [(new FakeTransport())->respond(fixture('v1-free/invalid_key.json'), 400), InvalidApiKeyException::class],
            [(new FakeTransport())->fail('boom')->fail('boom')->fail('boom'), NetworkException::class],
        ] as [$transport, $class]) {
            try {
                client($transport, self::PAID)->v1->lookup->team(1);
                self::fail("expected $class");
            } catch (\Throwable $e) {
                self::assertInstanceOf($class, $e);
                self::assertStringNotContainsString(self::PAID, $e->getMessage());
            }
        }
        try {
            client(new FakeTransport(static fn () => new Response(404, 'nope')), self::PAID)->v1->lookup->team(1);
        } catch (HttpStatusException $e) {
            self::assertStringContainsString('/api/v1/json/***/lookupteam.php?id=1', $e->getMessage());
        }
    }

    public function testUserAgentIsSent(): void
    {
        $t = (new FakeTransport())->respond(self::OK);
        client($t)->v1->list->sports();
        self::assertSame('thesportsdb-client-php', $t->requests[0][1]['User-Agent']);
    }

    public function testCacheServesRepeatsAndKeepsKeysOut(): void
    {
        $cache = new InMemoryResponseCache();
        $t = (new FakeTransport())->respond(self::OK);
        $c = client($t, self::PAID, cache: $cache);
        $c->v1->list->sports();
        $c->v1->list->sports();
        self::assertCount(1, $t->requests);
        self::assertCount(1, $cache);
        foreach ($cache->keys() as $key) {
            self::assertStringNotContainsString(self::PAID, $key);
        }
    }

    public function testLiveScoresAndEmptyBodiesAreNotCached(): void
    {
        $body = '{"livescore":[{"idLiveScore":"1"}]}';
        $t = (new FakeTransport())->respond($body)->respond($body)->respond('')->respond(self::OK);
        $c = client($t, cache: new InMemoryResponseCache());
        $c->v1->live->sport('Soccer');
        $c->v1->live->sport('Soccer');
        self::assertCount(2, $t->requests);
        self::assertSame([], $c->v1->list->sports());
        self::assertCount(1, $c->v1->list->sports());
    }

    public function testListenerSeesRetriesCacheHitsAndErrors(): void
    {
        $events = [];
        $t = (new FakeTransport())->respond('busy', 503)->respond(self::OK)->respond('gone', 404);
        $c = client($t, cache: new InMemoryResponseCache(), requestListener: function (RequestEvent $e) use (&$events): void {
            $events[] = $e;
        });
        $c->v1->list->sports();
        $c->v1->list->sports();
        try {
            $c->v1->lookup->team(1);
        } catch (HttpStatusException) {
        }
        [$retried, $cached, $failed] = $events;
        self::assertSame([2, 200, null], [$retried->attempts, $retried->status, $retried->error]);
        self::assertTrue($cached->fromCache);
        self::assertSame(0, $cached->attempts);
        self::assertSame(404, $failed->status);
        self::assertInstanceOf(HttpStatusException::class, $failed->error);
        self::assertStringEndsWith('/api/v1/json/***/lookupteam.php?id=1', $failed->url);
    }

    public function testAFailingListenerDoesNotBreakCalls(): void
    {
        $c = client((new FakeTransport())->respond(self::OK), requestListener: static function (): void {
            throw new \RuntimeException('listener bug');
        });
        self::assertCount(1, $c->v1->list->sports());
    }

    public function testASilentServerTimesOut(): void
    {
        // Accepts the connection but never answers: the call must give up, not hang.
        $server = stream_socket_server('tcp://127.0.0.1:0');
        self::assertNotFalse($server);
        $port = (int) substr((string) strrchr((string) stream_socket_get_name($server, false), ':'), 1);
        $db = new SportsDb(baseUrl: "http://127.0.0.1:$port", timeout: 0.3, maxRetries: 0, requestsPerMinute: 0);
        $started = microtime(true);
        try {
            $db->v1->list->sports();
            self::fail('expected NetworkException');
        } catch (NetworkException) {
        }
        self::assertLessThan(5, microtime(true) - $started);
        fclose($server);
    }
}
