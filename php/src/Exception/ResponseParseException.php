<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/** The body was not a TheSportsDB envelope (e.g. an HTML error page). */
final class ResponseParseException extends SportsDbException
{
}
