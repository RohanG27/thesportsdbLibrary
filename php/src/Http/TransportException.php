<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Http;

/** Thrown by a Transport when no HTTP response arrived. The client retries, then throws NetworkException. */
final class TransportException extends \RuntimeException
{
}
