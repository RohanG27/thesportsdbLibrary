package io.github.rohang27.thesportsdb;

import org.junit.jupiter.api.Test;
import io.github.rohang27.thesportsdb.cache.CachePolicy;
import io.github.rohang27.thesportsdb.cache.InMemoryResponseCache;
import io.github.rohang27.thesportsdb.model.Event;
import io.github.rohang27.thesportsdb.model.Team;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

/** The library as a Java caller sees it, through SportsDbFutures. */
class JavaInteropTest {
    private static SportsDbConfig config(FakeTransport transport, String key) {
        SportsDbConfig config = new SportsDbConfig();
        config.setApiKey(key);
        config.setTransport(transport);
        config.setRequestsPerMinute(0);
        config.setRetryBackoff(Duration.ZERO);
        config.setRateLimitWait(Duration.ofSeconds(1));
        config.setCache(new InMemoryResponseCache());
        config.setCachePolicy(CachePolicy.of(Duration.ofDays(7), Duration.ofDays(1), Duration.ofHours(1), Duration.ZERO));
        return config;
    }

    @Test
    void lookupsAndLists() {
        FakeTransport t = new FakeTransport();
        try (SportsDbFutures db = new SportsDbFutures(config(t, "123"))) {
            t.respond(TestSupportKt.fixture("v1-free/lookup_team.json"), 200, java.util.Map.of());
            Team team = db.v1().lookup().team(133604).join();
            assertEquals("Arsenal", team.getName());

            t.respond(TestSupportKt.fixture("v1-free/events_day.json"), 200, java.util.Map.of());
            // @JvmOverloads: optional arguments can be left out.
            List<Event> events = db.v1().schedule().day(LocalDate.of(2026, 10, 4)).join();
            assertFalse(events.isEmpty());

            t.respond("{\"events\":null}", 200, java.util.Map.of());
            assertNull(db.v1().lookup().event(1).join());
        }
    }

    @Test
    void errorsCompleteExceptionally() {
        try (SportsDbFutures db = new SportsDbFutures(config(new FakeTransport(), "123"))) {
            CompletionException e = assertThrows(CompletionException.class, () -> db.v2().all().sports().join());
            assertInstanceOf(PremiumRequiredException.class, e.getCause());
            assertFalse(db.isPremiumKey().join());
        }
    }

    @Test
    void v2WithPremiumKey() {
        FakeTransport t = new FakeTransport();
        try (SportsDbFutures db = new SportsDbFutures(config(t, "9999999999"))) {
            t.respond(TestSupportKt.fixture("v2/list_teams.json"), 200, java.util.Map.of());
            assertEquals(20, db.v2().list().teams(4328).join().size());
            t.respond(TestSupportKt.fixture("v2/filter_tv_channel.json"), 200, java.util.Map.of());
            assertFalse(db.v2().tv().channel("TSN 1").join().isEmpty());
            t.respond(TestSupportKt.fixture("v2/filter_tv_channel.json"), 200, java.util.Map.of());
            assertFalse(db.v2().tv().channel(8631L).join().isEmpty());
        }
    }
}
