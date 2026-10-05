"""Lenient readers for TheSportsDB records.

Everything arrives as text (or None), so each reader parses defensively: a malformed value
becomes None, never an exception. Mirrors the Kotlin library's ``Fields.kt``.
"""

from __future__ import annotations

import re
from collections.abc import Mapping
from datetime import date, datetime, time, timezone

_TIME = re.compile(r"^(\d{1,2}):(\d{2})(?::(\d{2}))?")
_DESCRIPTION = re.compile(r"^strDescription([A-Z]{2})$")


class Rec:
    """A normalized record (text or None values) with typed readers."""

    __slots__ = ("raw",)

    def __init__(self, raw: Mapping[str, str | None]) -> None:
        self.raw = raw

    def s(self, key: str) -> str | None:
        v = self.raw.get(key)
        return v if v is not None and v.strip() else None

    def i(self, key: str) -> int | None:
        v = self.s(key)
        if v is None:
            return None
        try:
            return int(v.strip())
        except ValueError:
            try:
                return int(float(v.strip()))
            except ValueError:
                return None

    def id_(self, key: str) -> int | None:
        """Ids: 0 means none."""
        v = self.i(key)
        return v if v else None

    def year(self, key: str) -> int | None:
        """Years: 0 means unknown."""
        v = self.i(key)
        return v if v and v > 0 else None

    def f(self, key: str) -> float | None:
        v = self.s(key)
        if v is None:
            return None
        try:
            return float(v.strip().removesuffix("%"))
        except ValueError:
            return None

    def b(self, key: str) -> bool | None:
        """``yes``/``no``, ``true``/``false``, ``1``/``0``, any case."""
        v = (self.s(key) or "").strip().lower()
        if v in ("yes", "true", "1", "y"):
            return True
        if v in ("no", "false", "0", "n"):
            return False
        return None

    def locked(self) -> bool | None:
        """``strLocked``: ``locked`` / ``unlocked``."""
        v = (self.s("strLocked") or "").strip().lower()
        return True if v == "locked" else False if v == "unlocked" else None

    def d(self, key: str) -> date | None:
        v = self.s(key)
        if v is None:
            return None
        try:
            return date.fromisoformat(v.strip()[:10])
        except ValueError:
            return None

    def t(self, key: str) -> time | None:
        """``16:00``, ``16:00:00``, ``16:00:00+00:00`` (any offset is ignored)."""
        v = self.s(key)
        m = _TIME.match(v.strip()) if v else None
        if not m:
            return None
        try:
            return time(int(m.group(1)), int(m.group(2)), int(m.group(3) or 0))
        except ValueError:
            return None

    def ts(self, key: str) -> datetime | None:
        """A UTC timestamp, timezone-aware. Accepts ``2026-10-10T11:30:00`` and ``2026-10-10 11:30:00``
        (no zone: UTC, as measured) and ISO strings with an offset or ``Z``."""
        v = self.s(key)
        if v is None:
            return None
        try:
            dt = datetime.fromisoformat(v.strip().replace(" ", "T").replace("Z", "+00:00"))
        except ValueError:
            return None
        return dt.replace(tzinfo=timezone.utc) if dt.tzinfo is None else dt.astimezone(timezone.utc)

    def ldt(self, key: str) -> datetime | None:
        """A date-time with no stated zone (e.g. ``dateUpdated``), returned naive."""
        v = self.s(key)
        if v is None:
            return None
        try:
            return datetime.fromisoformat(v.strip().replace(" ", "T")).replace(tzinfo=None)
        except ValueError:
            return None

    def csv(self, key: str) -> tuple[str, ...]:
        """A comma-separated list, trimmed, blanks dropped."""
        v = self.s(key)
        return tuple(p.strip() for p in v.split(",") if p.strip()) if v else ()

    def numbered(self, prefix: str, count: int) -> tuple[str, ...]:
        """Values of ``prefix1``..``prefixN`` that are present, in order."""
        return tuple(v for n in range(1, count + 1) if (v := self.s(f"{prefix}{n}")) is not None)

    def descriptions(self) -> dict[str, str]:
        """``strDescriptionEN``, ``strDescriptionDE``... keyed by language code (``EN``, ``DE``)."""
        out: dict[str, str] = {}
        for k in self.raw:
            m = _DESCRIPTION.match(k)
            if m and (v := self.s(k)) is not None:
                out[m.group(1)] = v
        return out
