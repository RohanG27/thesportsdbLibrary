<?php

declare(strict_types=1);

namespace SportsDb\Cache;

/** How quickly an endpoint's data changes. Every endpoint is tagged with one. */
enum Freshness
{
    /** Sports, countries, the league list. */
    case Static;
    /** Teams, players, venues, seasons. */
    case Slow;
    /** Schedules, standings, TV listings, highlights. */
    case Medium;
    /** Live scores. */
    case Live;
}
