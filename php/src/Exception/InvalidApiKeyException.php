<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/** The API rejected the key (HTTP 400 "Invalid Premium API key"). Only 123, 3 and paid keys work. */
final class InvalidApiKeyException extends SportsDbException
{
}
