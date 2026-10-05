<?php

declare(strict_types=1);

namespace SportsDb\Internal;

use SportsDb\Exception\ApiMessageException;
use SportsDb\Exception\InvalidApiKeyException;
use SportsDb\Exception\ResponseParseException;

/**
 * Pulls the records out of a response. Every response is one JSON object with one key holding a
 * list of records; the key differs by endpoint, so the expected key is tried first and any
 * list-valued key is the fallback. An empty body, {"key": null} and {"Message": "No data found"}
 * all mean "no results". A string under the record key is an error message.
 *
 * @internal
 */
final class Envelope
{
    /** @return list<array<string, string|null>> */
    public static function records(string $body, string $expectedKey, string $displayUrl): array
    {
        if (trim($body) === '') {
            return [];
        }
        try {
            $root = json_decode($body, true, 512, \JSON_THROW_ON_ERROR | \JSON_BIGINT_AS_STRING);
        } catch (\JsonException $e) {
            throw new ResponseParseException("Response from $displayUrl is not JSON: " . substr($body, 0, 120), 0, $e);
        }
        if (!\is_array($root) || array_is_list($root) && $root !== []) {
            throw new ResponseParseException("Response from $displayUrl is not a JSON object");
        }

        $message = $root['Message'] ?? null;
        if (\is_string($message)) {
            $lower = strtolower($message);
            if (str_starts_with($lower, 'no data found')) {
                return [];
            }
            if (str_starts_with($lower, 'invalid premium api key')) {
                throw new InvalidApiKeyException("TheSportsDB rejected the API key ($displayUrl): $message");
            }
            if (\count($root) === 1) {
                throw new ApiMessageException("TheSportsDB said: $message ($displayUrl)", $message);
            }
        }

        if (\array_key_exists($expectedKey, $root)) {
            $value = $root[$expectedKey];
        } else {
            $value = null;
            foreach ($root as $v) {
                if (\is_array($v) && array_is_list($v)) {
                    $value = $v;
                    break;
                }
            }
        }

        if ($value === null) {
            return [];
        }
        if (\is_string($value)) {
            // A rejected parameter: {"seasons": "Invalid League ID passed"}
            throw new ApiMessageException("TheSportsDB said: $value ($displayUrl)", $value);
        }
        if (!\is_array($value)) {
            throw new ResponseParseException("Unexpected '$expectedKey' value in response from $displayUrl");
        }
        if (!array_is_list($value)) {
            return [self::normalize($value)];
        }
        $out = [];
        foreach ($value as $record) {
            if (\is_array($record) && !array_is_list($record)) {
                $out[] = self::normalize($record);
            }
        }
        return $out;
    }

    /**
     * Every value as text or null: blank strings become null; JSON numbers (v2 search) become text.
     *
     * @param array<mixed> $record
     * @return array<string, string|null>
     */
    public static function normalize(array $record): array
    {
        $out = [];
        foreach ($record as $k => $v) {
            $out[(string) $k] = match (true) {
                $v === null, \is_array($v) => null,
                \is_bool($v) => $v ? 'true' : 'false',
                \is_int($v), \is_float($v) => (string) $v,
                \is_string($v) => trim($v) === '' ? null : $v,
                default => null,
            };
        }
        return $out;
    }
}
