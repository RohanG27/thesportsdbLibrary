import { describe, expect, it } from "vitest";
import { InvalidApiKeyError, PremiumRequiredError, type SportsDb } from "../src/index.js";
import { FakeTransport, PREMIUM, client, fixture } from "./support.js";

type Case = [fixture: string, path: string, run: (c: SportsDb) => Promise<unknown[]>, count: number];

const CASES: Case[] = [
  ["search_team", "search/team/Arsenal", (c) => c.v2.search.teams("Arsenal"), 12],
  ["search_league", "search/league/English%20Premier%20League", (c) => c.v2.search.leagues("English Premier League"), 2],
  ["search_player", "search/player/Danny%20Welbeck", (c) => c.v2.search.players("Danny Welbeck"), 1],
  ["search_venue", "search/venue/Wembley", (c) => c.v2.search.venues("Wembley"), 3],
  ["lookup_team_equipment", "lookup/team_equipment/133597", (c) => c.v2.lookup.teamEquipment(133597), 18],
  ["lookup_player_contracts", "lookup/player_contracts/34147178", (c) => c.v2.lookup.playerContracts(34147178), 1],
  ["lookup_player_results", "lookup/player_results/34160573", (c) => c.v2.lookup.playerResults(34160573), 24],
  ["lookup_player_honours", "lookup/player_honours/34147178", (c) => c.v2.lookup.playerHonours(34147178), 5],
  ["lookup_player_milestones", "lookup/player_milestones/34161397", (c) => c.v2.lookup.playerMilestones(34161397), 3],
  ["lookup_player_teams", "lookup/player_teams/34147178", (c) => c.v2.lookup.playerTeams(34147178), 6],
  ["lookup_player_stats", "lookup/player_stats/34146304", (c) => c.v2.lookup.playerStats(34146304), 315],
  ["lookup_event_lineup", "lookup/event_lineup/1032723", (c) => c.v2.lookup.eventLineup(1032723), 22],
  ["lookup_event_results", "lookup/event_results/652890", (c) => c.v2.lookup.eventResults(652890), 22],
  ["lookup_event_stats", "lookup/event_stats/1032723", (c) => c.v2.lookup.eventStats(1032723), 16],
  ["lookup_event_timeline", "lookup/event_timeline/1032718", (c) => c.v2.lookup.eventTimeline(1032718), 10],
  ["lookup_event_tv", "lookup/event_tv/2494052", (c) => c.v2.lookup.eventTv(2494052), 13],
  ["lookup_event_highlights", "lookup/event_highlights/441613", (c) => c.v2.lookup.eventHighlights(441613), 1],
  ["list_teams", "list/teams/4328", (c) => c.v2.list.teams(4328), 20],
  ["list_seasons", "list/seasons/4328", (c) => c.v2.list.seasons(4328), 35],
  ["list_players", "list/players/133604", (c) => c.v2.list.players(133604), 27],
  ["list_seasonposters", "list/seasonposters/4328", (c) => c.v2.list.seasonPosters(4328), 9],
  ["all_countries", "all/countries", (c) => c.v2.all.countries(), 256],
  ["all_sports", "all/sports", (c) => c.v2.all.sports(), 37],
  ["all_leagues", "all/leagues", (c) => c.v2.all.leagues(), 1547],
  ["schedule_next_league", "schedule/next/league/4328", (c) => c.v2.schedule.leagueNext(4328), 20],
  ["schedule_previous_league", "schedule/previous/league/4328", (c) => c.v2.schedule.leaguePrevious(4328), 20],
  ["schedule_next_team", "schedule/next/team/133604", (c) => c.v2.schedule.teamNext(133604), 10],
  ["schedule_previous_team", "schedule/previous/team/133604", (c) => c.v2.schedule.teamPrevious(133604), 10],
  ["schedule_next_venue", "schedule/next/venue/16163", (c) => c.v2.schedule.venueNext(16163), 3],
  ["schedule_previous_venue", "schedule/previous/venue/16163", (c) => c.v2.schedule.venuePrevious(16163), 10],
  ["schedule_full_team", "schedule/full/team/133604", (c) => c.v2.schedule.teamFull(133604), 48],
  ["schedule_league_season", "schedule/league/4328/2026-2027", (c) => c.v2.schedule.leagueSeason(4328, "2026-2027"), 380],
  ["filter_tv_day", "filter/tv/day/2026-10-05", (c) => c.v2.tv.day("2026-10-05"), 287],
  ["filter_tv_country", "filter/tv/country/Canada", (c) => c.v2.tv.country("Canada"), 90],
  ["filter_tv_sport", "filter/tv/sport/Ice%20Hockey", (c) => c.v2.tv.sport("Ice Hockey"), 189],
  ["filter_tv_channel", "filter/tv/channel/TSN%201", (c) => c.v2.tv.channel("TSN 1"), 3],
  ["filter_tv_channel_id", "filter/tv/channelid/8631", (c) => c.v2.tv.channelId(8631), 4],
  ["livescore_all", "livescore/all", (c) => c.v2.live.all(), 54],
  ["livescore_soccer", "livescore/soccer", (c) => c.v2.live.sport("soccer"), 17],
];

describe("v2 endpoints against recorded premium responses", () => {
  it.each(CASES)("%s", async (name, path, run, count) => {
    const t = new FakeTransport().respond(fixture(`v2/${name}.json`));
    const result = await run(client(t, PREMIUM));
    const [url, headers] = t.requests[0] ?? ["", {}];
    expect(url).toBe(`https://www.thesportsdb.com/api/v2/json/${path}`);
    expect(headers["X-API-KEY"]).toBe(PREMIUM);
    expect(url).not.toContain(PREMIUM);
    expect(result).toHaveLength(count);
  });
});

describe("v2 behaviour", () => {
  it("looks up single records", async () => {
    for (const [method, id, name] of [["league", 4328, "lookup_league"], ["team", 133604, "lookup_team"], ["player", 34145937, "lookup_player"],
      ["event", 441613, "lookup_event"], ["venue", 16163, "lookup_venue"]] as const) {
      const t = new FakeTransport().respond(fixture(`v2/${name}.json`));
      expect(await client(t, PREMIUM).v2.lookup[method](id)).not.toBeNull();
    }
  });

  it("reads ids sent as JSON numbers", async () => {
    const [team] = await client(new FakeTransport().respond(fixture("v2/search_team.json")), PREMIUM).v2.search.teams("Arsenal");
    expect(team?.id).toBe(133604);
    expect(team?.raw["idTeam"]).toBe("133604");
  });

  it("handles v2 specifics", async () => {
    const t = new FakeTransport().respond(fixture("v2/all_countries.json")).respond(fixture("v2/schedule_full_team.json"))
      .respond(fixture("v2/livescore_all.json")).respond(fixture("v2/search_event_none.json"));
    const c = client(t, PREMIUM);
    const andorra = (await c.v2.all.countries()).find((x) => x.name === "Andorra");
    expect([andorra?.code, andorra?.nameFr, andorra?.apiFootballId]).toEqual(["AD", "Andorre", null]);
    const [first] = await c.v2.schedule.teamFull(133604);
    expect(first?.timestamp?.toISOString()).toBe("2027-05-30T15:00:00.000Z");
    expect(first?.status).toBe("NOT_STARTED");
    const [live] = await c.v2.live.all();
    expect([live?.statusCode, live?.status, live?.homeScore]).toEqual(["P3", "IN_PLAY", 2]);
    expect(await c.v2.search.events("Arsenal vs Chelsea")).toEqual([]);
  });

  it.each(["123", "3"])("never calls v2 with free key %s", async (key) => {
    const t = new FakeTransport();
    const c = client(t, key);
    await expect(c.v2.all.sports()).rejects.toBeInstanceOf(PremiumRequiredError);
    expect(t.requests).toEqual([]);
    expect(await c.isPremiumKey()).toBe(false);
  });

  it("reports invalid keys and detects premium keys", async () => {
    expect(await client(new FakeTransport().respond(fixture("v2/lookup_league.json")), PREMIUM).isPremiumKey()).toBe(true);
    expect(await client(new FakeTransport().respond(fixture("v2/invalid_key.json"), 400), "1234567890").isPremiumKey()).toBe(false);
    await expect(client(new FakeTransport().respond(fixture("v2/invalid_key.json"), 400), "1234567890").v2.lookup.league(4328))
      .rejects.toBeInstanceOf(InvalidApiKeyError);
  });
});
