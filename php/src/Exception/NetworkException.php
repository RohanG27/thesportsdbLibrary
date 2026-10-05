<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/** No HTTP response arrived (DNS, connection, timeout), after retries. */
final class NetworkException extends SportsDbException
{
}
