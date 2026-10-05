<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Model;

/** A league a team plays in: one of idLeague/strLeague .. idLeague7/strLeague7. */
final readonly class LeagueRef
{
    /** @internal */
    public function __construct(public int $id, public ?string $name)
    {
    }
}
