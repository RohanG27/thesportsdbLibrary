<?php

declare(strict_types=1);

namespace SportsDb\V2;

use SportsDb\Cache\Freshness;
use SportsDb\Internal\Requester;
use SportsDb\Model\Country;
use SportsDb\Model\League;
use SportsDb\Model\Sport;

/** all/...: complete catalogues. */
final class All
{
    /** @internal */
    public function __construct(private readonly Requester $r)
    {
    }

    /** @return list<Country> */
    public function countries(): array
    {
        return $this->r->v2('all', Freshness::Static, static fn ($x) => new Country($x), ['all', 'countries']);
    }

    /** @return list<Sport> */
    public function sports(): array
    {
        return $this->r->v2('all', Freshness::Static, static fn ($x) => new Sport($x), ['all', 'sports']);
    }

    /** @return list<League> */
    public function leagues(): array
    {
        return $this->r->v2('all', Freshness::Static, static fn ($x) => new League($x), ['all', 'leagues']);
    }
}
