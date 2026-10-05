<?php

declare(strict_types=1);

namespace SportsDb\V1;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\TvListing;

/** TV listings. Free keys: 1 per call. */
final class Tv
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /**
     * eventstv.php?d=: listings for a day, optionally a sport, or a country **and** sport. The API
     * returns an empty body for a country without a sport, so that is rejected here.
     *
     * @return list<TvListing>
     */
    public function day(\DateTimeInterface $date, ?string $sport = null, ?string $country = null): array
    {
        if ($country !== null && $sport === null) {
            throw new \InvalidArgumentException('eventstv.php needs a sport when filtering by country');
        }
        return $this->listings(['d' => $date, 'a' => $country, 's' => $sport]);
    }

    /** @return list<TvListing> eventstv.php?c=: a channel by name (e.g. "TSN 1") */
    public function channel(string $name): array
    {
        return $this->listings(['c' => $name]);
    }

    /** @return list<TvListing> eventstv.php?id=: by **channel** id (TvListing::$channelId), not event id */
    public function channelId(int $channelId): array
    {
        return $this->listings(['id' => $channelId]);
    }

    /**
     * @param array<string, scalar|\DateTimeInterface|null> $params
     * @return list<TvListing>
     */
    private function listings(array $params): array
    {
        return $this->r->v1('eventstv.php', 'tvevents', Freshness::Medium, static fn ($x) => new TvListing($x), $params);
    }
}
