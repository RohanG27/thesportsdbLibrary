"""Pulls the records out of a TheSportsDB response.

Every response is one JSON object with one key whose value is a list of records. The key
differs by endpoint, so the expected key is tried first and any list-valued key is the
fallback. All of these mean "no results": an empty body, ``{"key": null}`` and
``{"Message": "No data found"}``. A string under the record key is an error message.
"""

from __future__ import annotations

import json
from typing import Any

from .errors import ApiMessageError, InvalidApiKeyError, ResponseParseError

Record = dict[str, "str | None"]

_NO_DATA = "no data found"
_INVALID_KEY = "invalid premium api key"


def parse_records(body: str, expected_key: str, display_url: str) -> list[Record]:
    if not body.strip():
        return []
    try:
        root: Any = json.loads(body)
    except ValueError as e:
        raise ResponseParseError(f"Response from {display_url} is not JSON: {body[:120]!r}") from e
    if not isinstance(root, dict):
        raise ResponseParseError(f"Response from {display_url} is not a JSON object")

    message = root.get("Message")
    if isinstance(message, str):
        lowered = message.lower()
        if lowered.startswith(_NO_DATA):
            return []
        if lowered.startswith(_INVALID_KEY):
            raise InvalidApiKeyError(f"TheSportsDB rejected the API key ({display_url}): {message}")
        if len(root) == 1:
            raise ApiMessageError(f"TheSportsDB said: {message} ({display_url})", message)

    if expected_key in root:
        value = root[expected_key]
    else:
        value = next((v for v in root.values() if isinstance(v, list)), None)

    if value is None:
        return []
    if isinstance(value, list):
        return [normalize(r) for r in value if isinstance(r, dict)]
    if isinstance(value, dict):
        return [normalize(value)]
    if isinstance(value, str):
        # A rejected parameter: {"seasons": "Invalid League ID passed"}
        raise ApiMessageError(f"TheSportsDB said: {value} ({display_url})", value)
    raise ResponseParseError(f"Unexpected {expected_key!r} value in response from {display_url}")


def normalize(record: dict[str, Any]) -> Record:
    """Every value as text or None: blank strings become None, and JSON numbers (v2 search) become text."""
    out: Record = {}
    for k, v in record.items():
        if v is None or isinstance(v, (dict, list)):
            out[k] = None
        elif isinstance(v, bool):
            out[k] = "true" if v else "false"
        else:
            text = str(v)
            out[k] = text if text.strip() else None
    return out
