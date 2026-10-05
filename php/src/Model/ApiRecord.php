<?php

declare(strict_types=1);

namespace SportsDb\Model;

/**
 * Base class of every record the API returns (Team, Event, ...).
 *
 * Records are read-only and built by the client from one API record. Every property is read
 * from $raw, so two records are equal (==) when the API sent the same fields. To test code that
 * uses them, fake the HTTP layer (SportsDb\Http\Transport) instead of building records.
 */
abstract readonly class ApiRecord implements \Stringable
{
    /**
     * @param array<string, string|null> $raw every field as the API sent it (blank strings as null);
     *        use it for fields not modelled yet: $team->raw['strKeywords']
     */
    public function __construct(public array $raw)
    {
    }

    /** True when $other is the same kind of record, built from the same API fields. */
    public function equals(?self $other): bool
    {
        return $other !== null && $other::class === static::class && $other->raw == $this->raw;
    }

    abstract public function __toString(): string;

    protected function fmt(mixed $value): string
    {
        return match (true) {
            $value === null => 'null',
            $value instanceof \DateTimeInterface => $value->format('Y-m-d\TH:i:s\Z'),
            \is_bool($value) => $value ? 'true' : 'false',
            \is_scalar($value) => (string) $value,
            default => get_debug_type($value),
        };
    }
}
