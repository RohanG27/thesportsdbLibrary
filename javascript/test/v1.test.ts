import { describe, expect, it } from "vitest";
import { ApiMessageError, type SportsDb } from "../src/index.js";
import { FakeTransport, PREMIUM, client, fixture } from "./support.js";

const D = "2026-10-04";
type Case = [fixture: string, call: string, run: (c: SportsDb) => Promise<unknown[]>, minimum: number];

const CASES: Case[] = [
  ["search_teams", "searchteams.php?t=Arsenal", (c) => c.v1.search.teams("Arsenal"), 1],
  ["search_events", "searchevents.php?e=Arsenal%20vs%20Chelsea", (c) => c.v1.search.events("Arsenal vs Chelsea"), 1],
  ["search_events_season", "searchevents.php?e=Arsenal_vs_Chelsea&s=2016-2017", (c) => c.v1.search.events("Arsenal_vs_Chelsea", { season: "2016-2017" }), 1],
  ["search_events_date", "searchevents.php?e=Arsenal%20vs%20Chelsea&d=2015-04-26", (c) => c.v1.search.events("Arsenal vs Chelsea", { date: new Date("2015-04-26") }), 1],
  ["search_filename", "searchfilename.php?e=English%20Premier%20League%202015-04-26%20Arsenal%20vs%20Chelsea",
    (c) => c.v1.search.eventsByFilename("English Premier League 2015-04-26 Arsenal vs Chelsea"), 1],
  ["search_players", "searchplayers.php?p=Danny%20Welbeck", (c) => c.v1.search.players("Danny Welbeck"), 1],
  ["search_venues", "searchvenues.php?v=Wembley", (c) => c.v1.search.venues("Wembley"), 1],
  ["lookup_table", "lookuptable.php?l=4328", (c) => c.v1.lookup.table(4328), 5],
  ["lookup_table_season", "lookuptable.php?l=4328&s=2024-2025", (c) => c.v1.lookup.table(4328, "2024-2025"), 5],
  ["lookup_equipment", "lookupequipment.php?id=133597", (c) => c.v1.lookup.equipment(133597), 1],
  ["lookup_honours", "lookuphonours.php?id=34147178", (c) => c.v1.lookup.honours(34147178), 1],
  ["lookup_former_teams", "lookupformerteams.php?id=34147178", (c) => c.v1.lookup.formerTeams(34147178), 1],
  ["lookup_milestones", "lookupmilestones.php?id=34161397", (c) => c.v1.lookup.milestones(34161397), 1],
  ["lookup_contracts", "lookupcontracts.php?id=34147178", (c) => c.v1.lookup.contracts(34147178), 1],
  ["player_results", "playerresults.php?id=34160573", (c) => c.v1.lookup.playerResults(34160573), 1],
  ["lookup_player_stats", "lookupplayerstats.php?id=34146304", (c) => c.v1.lookup.playerStats(34146304), 1],
  ["event_results", "eventresults.php?id=652890", (c) => c.v1.lookup.eventResults(652890), 1],
  ["lookup_lineup", "lookuplineup.php?id=1032723", (c) => c.v1.lookup.lineup(1032723), 1],
  ["lookup_timeline", "lookuptimeline.php?id=1032718", (c) => c.v1.lookup.timeline(1032718), 1],
  ["lookup_event_stats", "lookupeventstats.php?id=1032723", (c) => c.v1.lookup.eventStats(1032723), 1],
  ["lookup_tv", "lookuptv.php?id=2494052", (c) => c.v1.lookup.eventTv(2494052), 1],
  ["all_sports", "all_sports.php", (c) => c.v1.list.sports(), 2],
  ["all_countries", "all_countries.php", (c) => c.v1.list.countries(), 50],
  ["all_leagues", "all_leagues.php", (c) => c.v1.list.leagues(), 1],
  ["search_all_leagues", "search_all_leagues.php?c=England&s=Soccer", (c) => c.v1.list.leaguesInCountry("England", "Soccer"), 1],
  ["search_all_seasons", "search_all_seasons.php?id=4328", (c) => c.v1.list.seasons(4328), 1],
  ["search_all_seasons_poster", "search_all_seasons.php?id=4328&poster=1", (c) => c.v1.list.seasons(4328, { posters: true }), 1],
  ["search_all_seasons_badge", "search_all_seasons.php?id=4328&badge=1", (c) => c.v1.list.seasons(4328, { badges: true }), 1],
  ["search_all_seasons_description", "search_all_seasons.php?id=4328&description=1", (c) => c.v1.list.seasons(4328, { descriptions: true }), 1],
  ["search_all_teams_league", "search_all_teams.php?l=English%20Premier%20League", (c) => c.v1.list.teamsInLeague("English Premier League"), 10],
  ["search_all_teams_country", "search_all_teams.php?s=Soccer&c=Spain", (c) => c.v1.list.teamsInCountry("Soccer", "Spain"), 10],
  ["lookup_all_players", "lookup_all_players.php?id=133604", (c) => c.v1.list.players(133604), 10],
  ["events_next", "eventsnext.php?id=133602", (c) => c.v1.schedule.teamNext(133602), 1],
  ["events_last", "eventslast.php?id=133602", (c) => c.v1.schedule.teamLast(133602), 1],
  ["events_next_league", "eventsnextleague.php?id=4328", (c) => c.v1.schedule.leagueNext(4328), 1],
  ["events_past_league", "eventspastleague.php?id=4328", (c) => c.v1.schedule.leaguePast(4328), 1],
  ["events_day", "eventsday.php?d=2026-10-04", (c) => c.v1.schedule.day(D), 3],
  ["events_day_sport", "eventsday.php?d=2026-10-04&s=Ice%20Hockey", (c) => c.v1.schedule.day(new Date(D), { sport: "Ice Hockey" }), 3],
  ["events_season", "eventsseason.php?id=4328&s=2026-2027", (c) => c.v1.schedule.season(4328, "2026-2027"), 5],
  ["events_round", "eventsround.php?id=4328&r=1&s=2026-2027", (c) => c.v1.schedule.round(4328, 1, "2026-2027"), 10],
  ["events_tv_day", "eventstv.php?d=2026-10-05", (c) => c.v1.tv.day("2026-10-05"), 1],
  ["events_tv_country", "eventstv.php?d=2026-10-05&a=Canada&s=Ice%20Hockey", (c) => c.v1.tv.day("2026-10-05", { sport: "Ice Hockey", country: "Canada" }), 1],
  ["events_tv_channel", "eventstv.php?c=TSN%201", (c) => c.v1.tv.channel("TSN 1"), 1],
  ["events_tv_channel_id", "eventstv.php?id=8631", (c) => c.v1.tv.channelId(8631), 1],
  ["events_highlights", "eventshighlights.php?d=2026-10-04", (c) => c.v1.video.highlights(D), 1],
  ["events_highlights_sport", "eventshighlights.php?d=2026-10-04&s=Soccer", (c) => c.v1.video.highlights(D, { sport: "Soccer" }), 1],
  ["livescore_soccer", "livescore.php?s=Soccer", (c) => c.v1.live.sport("Soccer"), 1],
];

describe("v1 endpoints against recorded responses", () => {
  it.each(CASES)("%s", async (name, call, run, minimum) => {
    const t = new FakeTransport().respond(fixture(`v1-free/${name}.json`));
    const result = await run(client(t));
    expect(t.lastUrl).toBe(`https://www.thesportsdb.com/api/v1/json/123/${call}`);
    expect(result.length).toBeGreaterThanOrEqual(minimum);
  });
});

describe("v1 fields", () => {
  it("parses a team", async () => {
    const team = await client(new FakeTransport().respond(fixture("v1-free/lookup_team.json"))).v1.lookup.team(133604);
    expect([team?.id, team?.name, team?.shortName]).toEqual([133604, "Arsenal", "ARS"]);
    expect(team?.alternateNames).toContain("Arsenal FC");
    expect(team?.leagues[0]?.id).toBe(4328);
    expect(team?.leagues.length).toBeGreaterThan(1);
    expect(Object.keys(team?.descriptions ?? {})).toContain("DE");
    expect(team?.description).toBeTruthy();
    expect(team?.colours[0]).toBe("#EF0107");
    expect(team?.isLocked).toBe(false);
  });

  it("reads season times as UTC", async () => {
    const [first] = await client(new FakeTransport().respond(fixture("v1-free/events_season.json"))).v1.schedule.season(4328, "2026-2027");
    expect(first?.date).toBe("2026-08-21");
    expect(first?.time).toBe("19:00:00");
    expect(first?.timestamp?.toISOString()).toBe("2026-08-21T19:00:00.000Z");
  });

  it("knows older events have no status", async () => {
    const event = await client(new FakeTransport().respond(fixture("v1-free/lookup_event.json"))).v1.lookup.event(441613);
    expect(event?.statusCode).toBeNull();
    expect(event?.status).toBe("UNKNOWN");
    expect(event?.homeScore).toBe(4);
  });

  it("parses league, player and venue", async () => {
    const t = new FakeTransport().respond(fixture("v1-free/lookup_league.json")).respond(fixture("v1-free/lookup_player.json"))
      .respond(fixture("v1-free/lookup_venue.json"));
    const c = client(t);
    expect((await c.v1.lookup.league(4328))?.firstEventDate).toBe("1992-08-15");
    const player = await c.v1.lookup.player(34145937);
    expect(player?.born).toBeTruthy();
    expect(player?.externalIds.wikidata).toBeTruthy();
    const venue = await c.v1.lookup.venue(16163);
    expect(venue?.capacity).toBe(90000);
    expect(venue?.coordinates).toEqual([51.555556, -0.279444]);
  });

  it("reads TV timestamps with a space as UTC, and channel ids as channels", async () => {
    const tv = await client(new FakeTransport().respond(fixture("v1-free/lookup_tv.json"))).v1.lookup.eventTv(2494052);
    expect(tv.every((l) => l.timestamp instanceof Date)).toBe(true);
    const byChannel = await client(new FakeTransport().respond(fixture("v1-free/events_tv_channel_id.json"))).v1.tv.channelId(8631);
    expect(new Set(byChannel.map((l) => l.channelId))).toEqual(new Set([8631]));
  });

  it("treats empty results as empty and rejected parameters as errors", async () => {
    const t = new FakeTransport().respond(fixture("v1-free/events_day_none.json")).respond("")
      .respond(fixture("v1-free/search_all_seasons_bad_param.json"));
    const c = client(t);
    expect(await c.v1.schedule.day(D, { leagueId: 4328 })).toEqual([]);
    expect(await c.v1.tv.channel("x")).toEqual([]);
    await expect(c.v1.list.seasons(4328)).rejects.toMatchObject({ name: "ApiMessageError", apiMessage: "Invalid League ID passed" });
    await expect(client(new FakeTransport().respond(fixture("v1-free/events_round_bad_param.json"))).v1.schedule.round(4328, 1, "x"))
      .rejects.toBeInstanceOf(ApiMessageError);
  });

  it("checks argument rules before any request", async () => {
    const t = new FakeTransport();
    const c = client(t);
    await expect(c.v1.tv.day(D, { country: "Canada" })).rejects.toBeInstanceOf(RangeError);
    await expect(c.v1.search.events("x", { season: "2020", date: D })).rejects.toBeInstanceOf(RangeError);
    await expect(c.v1.list.seasons(1, { badges: true, posters: true })).rejects.toBeInstanceOf(RangeError);
    expect(t.requests).toEqual([]);
  });

  it("returns bigger results with a premium key", async () => {
    const t = new FakeTransport().respond(fixture("v1-premium/events_day.json")).respond(fixture("v1-premium/lookup_table.json"));
    const c = client(t, PREMIUM);
    expect((await c.v1.schedule.day(D)).length).toBeGreaterThan(500);
    expect((await c.v1.lookup.table(4328)).map((s) => s.rank)).toEqual([...Array(20).keys()].map((i) => i + 1));
  });
});
