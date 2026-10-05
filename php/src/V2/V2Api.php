<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\V2;

use RohanG27\TheSportsDb\Internal\Requester;

/**
 * The v2 API: https://www.thesportsdb.com/api/v2/json/{group}/{name}/{param}, key in the
 * X-API-KEY header. **Premium keys only**: with a free key every call throws
 * PremiumRequiredException without touching the network.
 */
final class V2Api
{
    public readonly Search $search;
    public readonly Lookup $lookup;
    public readonly Lists $list;
    public readonly All $all;
    public readonly Schedule $schedule;
    public readonly Tv $tv;
    public readonly Live $live;

    /** @internal */
    public function __construct(Requester $r)
    {
        $this->search = new Search($r);
        $this->lookup = new Lookup($r);
        $this->list = new Lists($r);
        $this->all = new All($r);
        $this->schedule = new Schedule($r);
        $this->tv = new Tv($r);
        $this->live = new Live($r);
    }
}
