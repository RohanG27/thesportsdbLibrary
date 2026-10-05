<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Exception;

/**
 * Base class for every exception this library throws. Messages never contain an API key:
 * v1 URLs (which carry the key in their path) are shown with *** in its place.
 */
abstract class SportsDbException extends \RuntimeException
{
}
