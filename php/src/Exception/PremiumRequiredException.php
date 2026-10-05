<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Exception;

/** A v2 endpoint was called with a free key. v2 accepts premium keys only. Thrown before any request. */
final class PremiumRequiredException extends SportsDbException
{
}
