<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Internal;

/**
 * Lenient readers for a normalized record (every value a string or null). A malformed value
 * becomes null, never an exception. Mirrors Fields.kt (Kotlin) and _fields.py (Python).
 *
 * @internal
 */
final class Rec
{
    private const UTC = 'UTC';

    /** @param array<string, string|null> $raw */
    public function __construct(public readonly array $raw)
    {
    }

    public function s(string $key): ?string
    {
        $v = $this->raw[$key] ?? null;
        return $v !== null && trim($v) !== '' ? $v : null;
    }

    public function int(string $key): ?int
    {
        $v = $this->s($key);
        if ($v === null) {
            return null;
        }
        $v = trim($v);
        if (preg_match('/^-?\d+$/', $v)) {
            return (int) $v;
        }
        return is_numeric($v) ? (int) (float) $v : null;
    }

    /** Ids: 0 means none. */
    public function id(string $key): ?int
    {
        $v = $this->int($key);
        return $v === 0 ? null : $v;
    }

    /** Years: 0 means unknown. */
    public function year(string $key): ?int
    {
        $v = $this->int($key);
        return $v !== null && $v > 0 ? $v : null;
    }

    public function float(string $key): ?float
    {
        $v = $this->s($key);
        if ($v === null) {
            return null;
        }
        $v = rtrim(trim($v), '%');
        return is_numeric($v) ? (float) $v : null;
    }

    /** yes/no, true/false, 1/0, any case. */
    public function bool(string $key): ?bool
    {
        return match (strtolower(trim($this->s($key) ?? ''))) {
            'yes', 'true', '1', 'y' => true,
            'no', 'false', '0', 'n' => false,
            default => null,
        };
    }

    /** strLocked: "locked" / "unlocked". */
    public function locked(): ?bool
    {
        return match (strtolower(trim($this->s('strLocked') ?? ''))) {
            'locked' => true,
            'unlocked' => false,
            default => null,
        };
    }

    /** A date (YYYY-MM-DD) as midnight UTC. */
    public function date(string $key): ?\DateTimeImmutable
    {
        $v = $this->s($key);
        if ($v === null || !preg_match('/^(\d{4})-(\d{2})-(\d{2})/', trim($v), $m) || !checkdate((int) $m[2], (int) $m[3], (int) $m[1])) {
            return null;
        }
        return new \DateTimeImmutable("{$m[1]}-{$m[2]}-{$m[3]}T00:00:00", new \DateTimeZone(self::UTC));
    }

    /** A time of day as HH:MM:SS. Accepts 16:00, 16:00:00, 16:00:00+00:00 (the offset is ignored). */
    public function time(string $key): ?string
    {
        $v = $this->s($key);
        if ($v === null || !preg_match('/^(\d{1,2}):(\d{2})(?::(\d{2}))?/', trim($v), $m)) {
            return null;
        }
        [$h, $i, $s] = [(int) $m[1], (int) $m[2], (int) ($m[3] ?? 0)];
        return $h < 24 && $i < 60 && $s < 60 ? sprintf('%02d:%02d:%02d', $h, $i, $s) : null;
    }

    /**
     * A UTC timestamp. Accepts 2026-10-10T11:30:00 and 2026-10-10 11:30:00 (no zone: UTC, as
     * measured) and ISO strings with an offset or Z. Returned in UTC.
     */
    public function instant(string $key): ?\DateTimeImmutable
    {
        $v = $this->s($key);
        if ($v === null || !preg_match('/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2})?(Z|[+-]\d{2}:?\d{2})?$/', trim($v))) {
            return null;
        }
        try {
            $dt = new \DateTimeImmutable(str_replace(' ', 'T', trim($v)), new \DateTimeZone(self::UTC));
        } catch (\Exception) {
            return null;
        }
        return $dt->setTimezone(new \DateTimeZone(self::UTC));
    }

    /** A date-time with no stated zone (e.g. dateUpdated), read as written. */
    public function localDateTime(string $key): ?\DateTimeImmutable
    {
        $v = $this->s($key);
        if ($v === null || !preg_match('/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2})?$/', trim($v))) {
            return null;
        }
        try {
            return new \DateTimeImmutable(str_replace(' ', 'T', trim($v)));
        } catch (\Exception) {
            return null;
        }
    }

    /** @return list<string> a comma-separated list, trimmed, blanks dropped */
    public function csv(string $key): array
    {
        $v = $this->s($key);
        return $v === null ? [] : array_values(array_filter(array_map('trim', explode(',', $v)), static fn ($p) => $p !== ''));
    }

    /** @return list<string> values of prefix1..prefixN that are present */
    public function numbered(string $prefix, int $count): array
    {
        $out = [];
        for ($n = 1; $n <= $count; $n++) {
            if (($v = $this->s($prefix . $n)) !== null) {
                $out[] = $v;
            }
        }
        return $out;
    }

    /** @return array<string, string> strDescriptionEN, strDescriptionDE... keyed by language code */
    public function descriptions(): array
    {
        $out = [];
        foreach ($this->raw as $k => $_) {
            if (preg_match('/^strDescription([A-Z]{2})$/', $k, $m) && ($v = $this->s($k)) !== null) {
                $out[$m[1]] = $v;
            }
        }
        return $out;
    }
}
