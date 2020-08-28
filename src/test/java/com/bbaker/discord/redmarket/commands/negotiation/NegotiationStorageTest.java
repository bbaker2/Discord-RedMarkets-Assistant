package com.bbaker.discord.redmarket.commands.negotiation;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbaker.discord.redmarket.db.DatabaseService;
import com.bbaker.discord.redmarket.db.DatabaseServiceImpl;
import com.bbaker.discord.redmarket.exceptions.SetupException;

class NegotiationStorageTest {

    private DatabaseService dbService = null;
    private NegotiationStorageImpl storage = null;


    @BeforeEach
    public void setupDatabase() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            fail("Failed right off the bat. Unable to load the driver");
        }
        Properties testProperties = new Properties();
        testProperties.setProperty("url", 	 "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        testProperties.setProperty("prefix", "test_");
        try {
            dbService = new DatabaseServiceImpl(testProperties);
            storage = new NegotiationStorageImpl(dbService);
            storage.createTable();
        } catch (SetupException e) {
            fail("Unable to prep the database service", e);
        }
    }

    @AfterEach
    public void dropAllTables() {
        dbService.withHandle(handle -> handle.execute("DROP ALL OBJECTS"));
    }


    @Test
    void testTrackerMissing() {
        Optional<Tracker> actual = storage.getTracker(222222);
        assertFalse(actual.isPresent(), "No record should be found, no error thrown");
    }

    @Test
    void testStoreTracker() {
        Tracker expected = new Tracker(1, 2, 3, 4, 5, 6, true);
        Tracker second = new Tracker(7, 8, 9, 10, 11, 12, false);
        storage.storeTracker(111111, expected);
        storage.storeTracker(222222, second); // a diversion insert

        Optional<Tracker> found = storage.getTracker(111111);
        assertThat(found).isPresent();
        Tracker actual = found.get();
        assertEquals(expected.getProviderTrack(),    actual.getProviderTrack());
        assertEquals(expected.getProvider(),         actual.getProvider());
        assertEquals(expected.getClient(),           actual.getClient());
        assertEquals(expected.getClientTrack(),      actual.getClientTrack());
        assertEquals(expected.getTotalRounds(),      actual.getTotalRounds());
        assertEquals(expected.getCurrentRound(),     actual.getCurrentRound());
        assertEquals(expected.getSwayClient(),       actual.getSwayClient());
        assertEquals(expected.getSwayProvider(),     actual.getSwayProvider());
    }

    @Test
    void testDoubleUpdate() {
        Tracker first = new Tracker(1, 2, 3, 4, 5, 6, true);
        Tracker second = new Tracker(7, 8, 9, 10, 11, 12, false);

        storage.storeTracker(111111,  first);
        storage.storeTracker(111111,  second);

        Optional<Tracker> found = storage.getTracker(111111);
        assertThat(found).isPresent();
        Tracker actual = found.get();
        assertEquals(second.getProviderTrack(),     actual.getProviderTrack());
        assertEquals(second.getProvider(),          actual.getProvider());
        assertEquals(second.getClient(),            actual.getClient());
        assertEquals(second.getClientTrack(),       actual.getClientTrack());
        assertEquals(second.getTotalRounds(),       actual.getTotalRounds());
        assertEquals(second.getCurrentRound(),      actual.getCurrentRound());
        assertEquals(second.getSwayClient(),        actual.getSwayClient());
        assertEquals(second.getSwayProvider(),      actual.getSwayProvider());


    }

}
