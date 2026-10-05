#!/usr/bin/env bash
# Records real TheSportsDB responses into src/test/resources/fixtures/ for the parser tests.
#
#   tools/record-fixtures.sh            # v1 with the public free key 123
#   THESPORTSDB_PREMIUM_KEY=... tools/record-fixtures.sh   # also v1-premium and v2
#   ONLY_MISSING=1 ...                  # skip fixtures that already exist (keeps tests stable)
#
# The premium key is sent only as v2's X-API-KEY header or inside a v1 URL that is
# never printed. Fixtures contain response bodies only, never keys.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=src/test/resources/fixtures
DELAY=${DELAY:-2.5}   # free tier allows 30 calls a minute

V1=(
  "search_teams|searchteams.php?t=Arsenal"
  "search_events|searchevents.php?e=Arsenal_vs_Chelsea"
  "search_events_season|searchevents.php?e=Arsenal_vs_Chelsea&s=2016-2017"
  "search_filename|searchfilename.php?e=English_Premier_League_2015-04-26_Arsenal_vs_Chelsea"
  "search_players|searchplayers.php?p=Danny_Welbeck"
  "search_venues|searchvenues.php?v=Wembley"
  "lookup_league|lookupleague.php?id=4328"
  "lookup_table|lookuptable.php?l=4328"
  "lookup_team|lookupteam.php?id=133604"
  "lookup_equipment|lookupequipment.php?id=133597"
  "lookup_player|lookupplayer.php?id=34145937"
  "lookup_honours|lookuphonours.php?id=34147178"
  "lookup_former_teams|lookupformerteams.php?id=34147178"
  "lookup_milestones|lookupmilestones.php?id=34161397"
  "lookup_contracts|lookupcontracts.php?id=34147178"
  "player_results|playerresults.php?id=34160573"
  "lookup_player_stats|lookupplayerstats.php?id=34146304"
  "lookup_event|lookupevent.php?id=441613"
  "event_results|eventresults.php?id=652890"
  "lookup_lineup|lookuplineup.php?id=1032723"
  "lookup_timeline|lookuptimeline.php?id=1032718"
  "lookup_event_stats|lookupeventstats.php?id=1032723"
  "lookup_tv|lookuptv.php?id=2494052"
  "lookup_venue|lookupvenue.php?id=16163"
  "all_sports|all_sports.php"
  "all_countries|all_countries.php"
  "all_leagues|all_leagues.php"
  "search_all_leagues|search_all_leagues.php?c=England&s=Soccer"
  "search_all_seasons|search_all_seasons.php?id=4328"
  "search_all_seasons_poster|search_all_seasons.php?id=4328&poster=1"
  "search_all_teams_league|search_all_teams.php?l=English_Premier_League"
  "search_all_teams_country|search_all_teams.php?s=Soccer&c=Spain"
  "lookup_all_players|lookup_all_players.php?id=133604"
  "events_next|eventsnext.php?id=133602"
  "events_last|eventslast.php?id=133602"
  "events_next_league|eventsnextleague.php?id=4328"
  "events_past_league|eventspastleague.php?id=4328"
  "events_day|eventsday.php?d=2026-10-04"
  "events_day_none|eventsday.php?d=2026-10-04&l=4328"
  "events_season|eventsseason.php?id=4328&s=2026-2027"
  "events_tv_day|eventstv.php?d=2026-10-05"
  "events_tv_country_no_sport|eventstv.php?d=2026-10-05&a=Canada"
  "events_tv_channel|eventstv.php?c=TSN_1"
  "events_tv_channel_id|eventstv.php?id=8631"
  "events_highlights|eventshighlights.php?d=2026-10-04"
  "livescore_soccer|livescore.php?s=Soccer"
  "livescore_league_ignored|livescore.php?l=4328"
  "search_events_date|searchevents.php?e=Arsenal_vs_Chelsea&d=2015-04-26"
  "search_events_f|searchevents.php?f=English_Premier_League_2015-04-26_Arsenal_vs_Chelsea"
  "search_filename_season|searchfilename.php?e=English_Premier_League_2015-04-26_Arsenal_vs_Chelsea&s=2014-2015"
  "lookup_table_season|lookuptable.php?l=4328&s=2024-2025"
  "search_all_seasons_badge|search_all_seasons.php?id=4328&badge=1"
  "search_all_seasons_description|search_all_seasons.php?id=4328&description=1"
  "events_day_sport|eventsday.php?d=2026-10-04&s=Ice_Hockey"
  "events_tv_day_sport|eventstv.php?d=2026-10-05&s=Ice_Hockey"
  "events_tv_country|eventstv.php?d=2026-10-05&a=Canada&s=Ice_Hockey"
  "events_highlights_league|eventshighlights.php?d=2026-10-04&l=4328"
  "events_highlights_sport|eventshighlights.php?d=2026-10-04&s=Soccer"
  "events_round|eventsround.php?id=4328&r=1&s=2026-2027"
  "events_round_bad_param|eventsround.php?l=4328&r=1&s=2026-2027"
  "search_all_seasons_bad_param|search_all_seasons.php?l=4328"
  "lookup_all_teams_wrong_league|lookup_all_teams.php?id=4328"
)

V2=(
  "search_league|search/league/English%20Premier%20League"
  "search_team|search/team/Arsenal"
  "search_player|search/player/Danny%20Welbeck"
  "search_event_none|search/event/Arsenal%20vs%20Chelsea"
  "search_venue|search/venue/Wembley"
  "lookup_league|lookup/league/4328"
  "lookup_team|lookup/team/133604"
  "lookup_team_equipment|lookup/team_equipment/133597"
  "lookup_player|lookup/player/34145937"
  "lookup_player_contracts|lookup/player_contracts/34147178"
  "lookup_player_results|lookup/player_results/34160573"
  "lookup_player_honours|lookup/player_honours/34147178"
  "lookup_player_milestones|lookup/player_milestones/34161397"
  "lookup_player_teams|lookup/player_teams/34147178"
  "lookup_player_stats|lookup/player_stats/34146304"
  "lookup_event|lookup/event/441613"
  "lookup_event_lineup|lookup/event_lineup/1032723"
  "lookup_event_results|lookup/event_results/652890"
  "lookup_event_stats|lookup/event_stats/1032723"
  "lookup_event_timeline|lookup/event_timeline/1032718"
  "lookup_event_tv|lookup/event_tv/2494052"
  "lookup_event_highlights|lookup/event_highlights/441613"
  "lookup_venue|lookup/venue/16163"
  "list_teams|list/teams/4328"
  "list_seasons|list/seasons/4328"
  "list_seasonposters|list/seasonposters/4328"
  "list_players|list/players/133604"
  "filter_tv_day|filter/tv/day/2026-10-05"
  "filter_tv_country|filter/tv/country/Canada"
  "filter_tv_channel|filter/tv/channel/TSN%201"
  "all_countries|all/countries"
  "all_sports|all/sports"
  "schedule_next_league|schedule/next/league/4328"
  "schedule_full_team|schedule/full/team/133604"
  "schedule_league_season|schedule/league/4328/2026-2027"
  "livescore_all|livescore/all"
  "livescore_soccer|livescore/soccer"
  "livescore_league|livescore/4328"
  "filter_tv_sport|filter/tv/sport/Ice%20Hockey"
  "filter_tv_channel_id|filter/tv/channelid/8631"
  "all_leagues|all/leagues"
  "schedule_previous_league|schedule/previous/league/4328"
  "schedule_next_team|schedule/next/team/133604"
  "schedule_previous_team|schedule/previous/team/133604"
  "schedule_next_venue|schedule/next/venue/16163"
  "schedule_previous_venue|schedule/previous/venue/16163"
)

fetch() { # dir name url [header]
  local dir=$1 name=$2 url=$3 hdr=${4:-}
  mkdir -p "$OUT/$dir"
  if [[ -n ${ONLY_MISSING:-} && -e "$OUT/$dir/$name.json" ]]; then return; fi
  local code
  if [[ -n $hdr ]]; then
    code=$(curl -s -o "$OUT/$dir/$name.json" -w '%{http_code}' -H "$hdr" "$url")
  else
    code=$(curl -s -o "$OUT/$dir/$name.json" -w '%{http_code}' "$url")
  fi
  printf '%-4s %s/%s (%s bytes)\n' "$code" "$dir" "$name" "$(wc -c < "$OUT/$dir/$name.json")"
  sleep "$DELAY"
}

for e in "${V1[@]}"; do
  fetch v1-free "${e%%|*}" "https://www.thesportsdb.com/api/v1/json/123/${e#*|}"
done
# A wrong key, to pin down the error shape; and "3", the other key that still works for free.
fetch v1-free invalid_key "https://www.thesportsdb.com/api/v1/json/1/lookupleague.php?id=4328"
fetch v1-free key3_all_sports "https://www.thesportsdb.com/api/v1/json/3/all_sports.php"

if [[ -n ${THESPORTSDB_PREMIUM_KEY:-} ]]; then
  for e in "${V1[@]}"; do
    fetch v1-premium "${e%%|*}" "https://www.thesportsdb.com/api/v1/json/$THESPORTSDB_PREMIUM_KEY/${e#*|}"
  done
  for e in "${V2[@]}"; do
    fetch v2 "${e%%|*}" "https://www.thesportsdb.com/api/v2/json/${e#*|}" "X-API-KEY: $THESPORTSDB_PREMIUM_KEY"
  done
else
  echo "THESPORTSDB_PREMIUM_KEY not set: skipped v1-premium and v2."
fi
fetch v2 invalid_key "https://www.thesportsdb.com/api/v2/json/lookup/league/4328" "X-API-KEY: 123"
