<?php

declare(strict_types=1);

namespace SportsDb\Model;

/**
 * A broad reading of a strStatus code, so you don't need every sport's codes. Covers the codes in
 * TheSportsDB's data documentation (docs_api_data) for every sport.
 */
enum EventStatus
{
    case NotStarted;
    case InPlay;
    case Finished;
    case Postponed;
    /** Suspended or interrupted (SUSP, INT, INTR): stopped, and may resume. */
    case Interrupted;
    case Cancelled;
    case Abandoned;
    /** Null, or a code this library doesn't know. The raw code is still on the record. */
    case Unknown;

    private const CODES = [
        'NS' => self::NotStarted, 'TBD' => self::NotStarted, 'NOT STARTED' => self::NotStarted, 'SCHEDULED' => self::NotStarted,
        'FT' => self::Finished, 'AET' => self::Finished, 'PEN' => self::Finished, 'AOT' => self::Finished, 'AP' => self::Finished,
        'FINISHED' => self::Finished, 'MATCH FINISHED' => self::Finished, 'FINAL' => self::Finished, 'ENDED' => self::Finished,
        'AWD' => self::Finished, 'AW' => self::Finished, 'WO' => self::Finished,
        'PST' => self::Postponed, 'POST' => self::Postponed, 'POSTPONED' => self::Postponed, 'DELAYED' => self::Postponed,
        'SUSP' => self::Interrupted, 'INT' => self::Interrupted, 'INTR' => self::Interrupted, 'SUSPENDED' => self::Interrupted,
        'INTERRUPTED' => self::Interrupted,
        'CANC' => self::Cancelled, 'CANCELLED' => self::Cancelled, 'CANCELED' => self::Cancelled,
        'ABD' => self::Abandoned, 'ABANDONED' => self::Abandoned,
        '1H' => self::InPlay, '2H' => self::InPlay, 'HT' => self::InPlay, 'ET' => self::InPlay, 'BT' => self::InPlay,
        'P' => self::InPlay, 'PT' => self::InPlay, 'LIVE' => self::InPlay, 'BREAK' => self::InPlay, 'Q1' => self::InPlay,
        'Q2' => self::InPlay, 'Q3' => self::InPlay, 'Q4' => self::InPlay, 'OT' => self::InPlay, 'P1' => self::InPlay,
        'P2' => self::InPlay, 'P3' => self::InPlay, 'SO' => self::InPlay, 'IN PROGRESS' => self::InPlay,
    ];

    /** Classifies a raw code (NS, 2H, Q3, P2, IN4, S2, FT, PST...). */
    public static function of(?string $code): self
    {
        $c = strtoupper(trim($code ?? ''));
        if ($c === '') {
            return self::Unknown;
        }
        return self::CODES[$c] ?? (preg_match('/^(IN\d+|S\d)$/', $c) ? self::InPlay : self::Unknown); // baseball innings, volleyball sets
    }
}
