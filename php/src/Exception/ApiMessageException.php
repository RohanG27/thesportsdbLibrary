<?php

declare(strict_types=1);

namespace SportsDb\Exception;

/**
 * The API answered with a message instead of data: an unrecognised {"Message": ...} body, or a
 * rejected parameter, which v1 reports as text where the records belong:
 * {"seasons": "Invalid League ID passed"}.
 */
final class ApiMessageException extends SportsDbException
{
    public function __construct(string $message, public readonly string $apiMessage)
    {
        parent::__construct($message);
    }
}
