<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Cache;

use Psr\SimpleCache\CacheInterface;

/** Caches responses in any PSR-16 cache (Redis, APCu, files...). Needs psr/simple-cache. */
final class Psr16ResponseCache implements ResponseCache
{
    public function __construct(private readonly CacheInterface $cache, private readonly string $prefix = 'thesportsdb.')
    {
    }

    public function get(string $key): ?string
    {
        $value = $this->cache->get($this->key($key));
        return \is_string($value) ? $value : null;
    }

    public function put(string $key, string $body, float $ttl): void
    {
        if ($ttl > 0) {
            $this->cache->set($this->key($key), $body, max(1, (int) ceil($ttl)));
        }
    }

    /** PSR-16 keys may not contain {}()/\@: so the request is hashed. */
    private function key(string $key): string
    {
        return $this->prefix . hash('sha256', $key);
    }
}
