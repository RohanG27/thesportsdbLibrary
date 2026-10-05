<?php

declare(strict_types=1);

namespace SportsDb\V1;

use SportsDb\Internal\Requester;

/**
 * The v1 API: https://www.thesportsdb.com/api/v1/json/{key}/{endpoint}.php
 *
 * Works with the free keys (small result limits, noted on each method) and with premium keys.
 * Names may contain spaces; the library encodes them.
 */
final class V1Api
{
    public readonly Search $search;
    public readonly Lookup $lookup;
    public readonly Lists $list;
    public readonly Schedule $schedule;
    public readonly Tv $tv;
    public readonly Video $video;
    public readonly Live $live;

    /** @internal */
    public function __construct(Requester $r)
    {
        $this->search = new Search($r);
        $this->lookup = new Lookup($r);
        $this->list = new Lists($r);
        $this->schedule = new Schedule($r);
        $this->tv = new Tv($r);
        $this->video = new Video($r);
        $this->live = new Live($r);
    }
}
