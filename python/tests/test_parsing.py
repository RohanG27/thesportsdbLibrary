from __future__ import annotations

from datetime import date, datetime, time, timezone

import pytest

from thesportsdb_client import ApiMessageError, EventStatus, ImageSize, InvalidApiKeyError, ResponseParseError, sized
from thesportsdb_client._envelope import normalize, parse_records
from thesportsdb_client._fields import Rec


def rec(**values: object) -> Rec:
    return Rec(normalize(dict(values)))


def test_blanks_and_nulls_are_missing() -> None:
    r = rec(a="", b=None, c="  ", d="x")
    assert (r.s("a"), r.s("b"), r.s("c"), r.s("missing"), r.s("d")) == (None, None, None, None, "x")


def test_numbers() -> None:
    r = rec(i="42", f="3.0", bad="n/a", zero="0", num=133604)
    assert (r.i("i"), r.i("f"), r.i("bad"), r.year("zero"), r.id_("zero"), r.id_("num")) == (42, 3, None, None, None, 133604)


def test_booleans_in_any_case() -> None:
    r = rec(a="Yes", b="NO", c="no", d="maybe", strLocked="unlocked")
    assert (r.b("a"), r.b("b"), r.b("c"), r.b("d"), r.locked()) == (True, False, False, None, False)


def test_timestamps_are_utc() -> None:
    r = rec(events="2026-10-10T11:30:00", tv="2026-10-10 11:30:00", zoned="2026-10-10T11:30:00+02:00", bad="soon")
    utc = datetime(2026, 10, 10, 11, 30, tzinfo=timezone.utc)
    assert r.ts("events") == utc and r.ts("tv") == utc
    assert r.ts("zoned") == datetime(2026, 10, 10, 9, 30, tzinfo=timezone.utc)
    assert r.ts("bad") is None


def test_dates_and_times() -> None:
    r = rec(d="2026-10-04", zero="0000-00-00", t1="16:00", t2="16:30:15", t3="16:00:00+00:00", tb="TBC")
    assert r.d("d") == date(2026, 10, 4) and r.d("zero") is None
    assert (r.t("t1"), r.t("t2"), r.t("t3"), r.t("tb")) == (time(16), time(16, 30, 15), time(16), None)


def test_comma_lists() -> None:
    assert rec(x="Arsenal Football Club, AFC, Arsenal FC").csv("x") == ("Arsenal Football Club", "AFC", "Arsenal FC")
    assert rec(x=None).csv("x") == ()


def test_envelopes() -> None:
    assert parse_records("", "events", "u") == []
    assert parse_records('{"events":null}', "events", "u") == []
    assert parse_records('{"Message":"No data found"}', "lookup", "u") == []
    assert len(parse_records('{"events":[{"idEvent":"1"}]}', "events", "u")) == 1
    assert parse_records('{"renamed":[{"idEvent":"1"}]}', "events", "u")[0]["idEvent"] == "1"  # key fallback
    with pytest.raises(ApiMessageError) as e:
        parse_records('{"seasons":"Invalid League ID passed"}', "seasons", "u")
    assert e.value.api_message == "Invalid League ID passed"
    with pytest.raises(ApiMessageError):
        parse_records('{"Message":"Something new"}', "x", "u")
    with pytest.raises(InvalidApiKeyError):
        parse_records('{"Message":"Invalid Premium API key: Signup here"}', "x", "u")
    with pytest.raises(ResponseParseError):
        parse_records("<html>Cloudflare</html>", "x", "u")


@pytest.mark.parametrize("code,status", [
    ("NS", EventStatus.NOT_STARTED), ("2H", EventStatus.IN_PLAY), ("Q3", EventStatus.IN_PLAY), ("P2", EventStatus.IN_PLAY),
    ("IN7", EventStatus.IN_PLAY), ("FT", EventStatus.FINISHED), ("aet", EventStatus.FINISHED), ("PST", EventStatus.POSTPONED),
    (None, EventStatus.UNKNOWN), ("???", EventStatus.UNKNOWN),
    # Codes from TheSportsDB's data documentation that the first version missed or misread.
    ("POST", EventStatus.POSTPONED), ("S3", EventStatus.IN_PLAY), ("PT", EventStatus.IN_PLAY), ("AW", EventStatus.FINISHED),
    ("INT", EventStatus.INTERRUPTED), ("INTR", EventStatus.INTERRUPTED), ("SUSP", EventStatus.INTERRUPTED),
])
def test_event_status(code: str | None, status: EventStatus) -> None:
    assert EventStatus.of(code) is status


def test_image_sizes() -> None:
    badge = "https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png"
    assert sized(badge, ImageSize.TINY) == badge + "/tiny"
    assert sized(badge + "/tiny", ImageSize.SMALL) == badge + "/small"
    honour = "https://www.thesportsdb.com/images/media/honour/logo/mxhjar1650460067.png"
    assert sized(honour, ImageSize.TINY) == honour + "/tiny"
    thumb = "https://www.thesportsdb.com/images/sports/soccer.jpg"  # /tiny 404s here
    assert sized(thumb, ImageSize.TINY) == thumb
    assert sized(None, ImageSize.TINY) is None


def test_round_stages() -> None:
    from thesportsdb_client import RoundStage

    assert RoundStage.of(200) is RoundStage.FINAL
    assert RoundStage.of(500) is RoundStage.PRE_SEASON
    assert RoundStage.of(38) is None
    assert RoundStage.of(226) is None  # seen in the data; not a documented code
    assert RoundStage.of(None) is None
