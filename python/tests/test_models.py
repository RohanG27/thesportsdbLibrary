from __future__ import annotations

import dataclasses
import re
from datetime import date

import pytest

from .conftest import FakeTransport, client, fixture


def test_records_are_equal_when_the_api_sent_the_same_data() -> None:
    t = FakeTransport().respond(fixture("v1-free/lookup_team.json")).respond(fixture("v1-free/lookup_team.json"))
    c = client(t)
    a, b = c.v1.lookup.team(133604), c.v1.lookup.team(133604)
    assert a == b and hash(a) == hash(b) and {a} == {b}


def test_different_types_from_the_same_fields_differ() -> None:
    body = '{"tvhighlights":[{"idEvent":"1"}],"tvevents":[{"idEvent":"1"}]}'
    c = client(FakeTransport().respond(body).respond(body))
    event = c.v1.video.highlights(date(2026, 1, 1))[0]
    listing = c.v1.tv.channel("x")[0]
    assert dict(event.raw) == dict(listing.raw) and event != listing


def test_repr_is_short() -> None:
    c = client(FakeTransport().respond(fixture("v1-free/lookup_team.json")).respond(fixture("v1-free/lookup_table.json")))
    assert repr(c.v1.lookup.team(133604)) == "Team(id=133604, name='Arsenal')"
    assert re.fullmatch(r"Standing\(rank=1, team='[^']+', points=\d+\)", repr(c.v1.lookup.table(4328)[0]))


def test_records_are_read_only() -> None:
    team = client(FakeTransport().respond(fixture("v1-free/lookup_team.json"))).v1.lookup.team(133604)
    assert team is not None
    with pytest.raises(dataclasses.FrozenInstanceError):
        team.name = "x"  # type: ignore[misc]
    with pytest.raises(TypeError):
        team.raw["strTeam"] = "x"  # type: ignore[index]
