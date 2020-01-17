package redmarket.commands.channel;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbaker.discord.redmarket.commands.channel.ChannelStorage;
import com.bbaker.discord.redmarket.commands.channel.ChannelStorageImpl;
import com.bbaker.discord.redmarket.db.DatabaseService;
import com.bbaker.discord.redmarket.db.DatabaseServiceImpl;
import com.bbaker.discord.redmarket.exceptions.SetupException;

class ChannelStorageTest {

    private static long USER_A = 11111111;
    private static long USER_B = 22222222;

    private static long CHANNEL_A = 33333333;
    private static long CHANNEL_B = 44444444;

    private DatabaseService dbService = null;
    private ChannelStorage storage = null;


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
            storage = new ChannelStorageImpl(dbService);
            storage.createTables();
        } catch (SetupException e) {
            fail("Unable to prep the database service", e);
        }
    }

    @AfterEach
    public void dropAllTables() {
        dbService.withHandle(handle -> handle.execute("DROP ALL OBJECTS"));
    }

    @Test
    void testRegister() {
        storage.registerChannel(CHANNEL_A, USER_A, tomorrow());

        long actual = storage.getOwner(CHANNEL_A).get();

        assertEquals(USER_A, actual, "The same user should be detected as the creator");
    }

    @Test
    void testUnregister() {
        storage.registerChannel(CHANNEL_A, USER_A, tomorrow());
        storage.unregisterChannel(CHANNEL_A);

        storage.getOwner(CHANNEL_A).ifPresent(owner -> fail("This channel should have been removed"));
    }

    @Test
    void testPurgeChannels() {
        storage.registerChannel(CHANNEL_A, USER_A, tomorrow());
        storage.registerChannel(CHANNEL_B, USER_B, tomorrow());

        storage.persistOnly(new long[] {CHANNEL_B});

        storage.getOwner(CHANNEL_A).ifPresent(o -> fail("This channel should have been cleaned out"));
        assertTrue(storage.getOwner(CHANNEL_B).isPresent(), "This channel should have been persisted");
    }

    private LocalDateTime tomorrow() {
        return LocalDateTime.now().plusDays(1);
    }

}
