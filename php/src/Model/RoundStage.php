<?php

declare(strict_types=1);

namespace SportsDb\Model;

/** The stage a special intRound value stands for (docs_api_data). Other values are ordinary round numbers. */
enum RoundStage: int
{
    case QuarterFinal = 125;
    case SemiFinal = 150;
    case Playoff = 160;
    case PlayoffSemiFinal = 170;
    case PlayoffFinal = 180;
    case Final = 200;
    case Qualifier = 400;
    case PreSeason = 500;

    /** The stage for an intRound value, or null for an ordinary round number. */
    public static function of(?int $round): ?self
    {
        return $round === null ? null : self::tryFrom($round);
    }
}
