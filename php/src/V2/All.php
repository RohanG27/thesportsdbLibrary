<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Cache\Freshness;
use RohanG27\TheSportsDb\Internal\Requester;
use RohanG27\TheSportsDb\Model\Country;
use RohanG27\TheSportsDb\Model\League;
use RohanG27\TheSportsDb\Model\Sport;

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
