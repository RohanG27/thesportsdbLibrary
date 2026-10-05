<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\TvListing;

/** filter/tv/... */
final class Tv
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<TvListing> every listing worldwide on a day */
    public function day(\DateTimeInterface $date): array
    {
        return $this->get('day', $date);
    }

    /** @return list<TvListing> about a week of listings for a country, e.g. "Canada" */
    public function country(string $country): array
    {
        return $this->get('country', $country);
    }

    /** @return list<TvListing> */
    public function sport(string $sport): array
    {
        return $this->get('sport', $sport);
    }

    /** @return list<TvListing> by channel name in the API's spelling ("TSN 1", not "TSN1"); partial names match */
    public function channel(string $name): array
    {
        return $this->get('channel', $name);
    }

    /** @return list<TvListing> */
    public function channelId(int $channelId): array
    {
        return $this->get('channelid', $channelId);
    }

    /** @return list<TvListing> */
    private function get(string $kind, string|int|\DateTimeInterface $value): array
    {
        return $this->r->v2('filter', Freshness::Medium, static fn ($x) => new TvListing($x), ['filter', 'tv', $kind, $value]);
    }
}
