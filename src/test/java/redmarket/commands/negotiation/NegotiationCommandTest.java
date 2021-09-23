package redmarket.commands.negotiation;

import static com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand.BEGIN_NEW_TRACKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationStorage;
import com.bbaker.discord.redmarket.commands.negotiation.Phase;
import com.bbaker.discord.redmarket.commands.negotiation.Tracker;
import com.bbaker.discord.redmarket.exceptions.BadFormatException;

import redmarket.CommonMocks;

class NegotiationCommandTest extends CommonMocks {
    private DiscordApi api;
    private NegotiationStorage storage;
    private Map<Long, Tracker> db;

    private NegotiationCommand cmd;

    @BeforeEach
    void setup() {
        api = mock(DiscordApi.class);
        storage = mock(NegotiationStorage.class);

        cmd = new NegotiationCommand(api, storage);

        db = new HashMap<Long, Tracker>();
        doAnswer(i->{
            long key = i.getArgument(0, Long.class);
            Tracker val = i.getArgument(1, Tracker.class);
            db.put(key, val);
            return null;
        }).when(storage).storeTracker(anyLong(), any(Tracker.class));

        doAnswer(i -> {
            long key = i.getArgument(0, Long.class);
            if(db.containsKey(key)) {
                return Optional.of(db.get(key));
            }
            return Optional.empty();
        }).when(storage).getTracker(anyLong());
    }

    @Test
    public void testStartSuccess() throws BadFormatException {
        // run
        cmd.start(4,6,3, CHANNEL);

        // validate
        verify(storage).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(Phase.NEGOTIATION, tracker.getPhase());
        assertEquals(5, tracker.getTotalRounds());
        assertEquals(1, tracker.getCurrentRound());
        assertEquals(1, tracker.getProvider());
        assertEquals(6, tracker.getClient());
        assertEquals(false, tracker.isSecret());

    }

    @Test
    public void testStartCritSuccess() throws BadFormatException {
        // run
        cmd.start(4,4,3, CHANNEL);

        // validate
        verify(storage).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(Phase.NEGOTIATION, tracker.getPhase());
        assertEquals(4, tracker.getTotalRounds());
        assertEquals(1, tracker.getCurrentRound());
        assertEquals(2, tracker.getProvider());
        assertEquals(6, tracker.getClient());
        assertEquals(false, tracker.isSecret());

    }

    @Test
    public void testStartCritFailed() throws BadFormatException {
        // run
        cmd.start(5,5,3, CHANNEL);

        // validate
        verify(storage).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(Phase.NEGOTIATION, tracker.getPhase());
        assertEquals(1, tracker.getCurrentRound());
        assertEquals(0, tracker.getProvider());
        assertEquals(6, tracker.getClient());
        assertEquals(true, tracker.isSecret());

    }

    @Test
    public void testTrackerlessStatus() {
        // run
        String reply = cmd.status(CHANNEL);

        // validate
        assertEquals(BEGIN_NEW_TRACKER, reply);
    }

    @Test
    public void testStatus() {
        // setup
        Tracker tracker = new Tracker(2, 5, 4, 2, 1, 1, false, Phase.NEGOTIATION);
        db.put(CHANNEL_ID, tracker);

        // run
        String reply = cmd.status(CHANNEL);

        // validate
        assertNotEquals(BEGIN_NEW_TRACKER, reply);
    }

    @Test
    public void testTracklessNext() throws BadFormatException {
        // run
        String reply = cmd.nextRound(1, 1, CHANNEL);

        // validate
        assertEquals(BEGIN_NEW_TRACKER, reply);
    }

    @CsvSource({
        "1,	NEGOTIATION, 2, NEGOTIATION,	1",
        "3,	NEGOTIATION, 4, CLOSING,		1",
        "4,	CLOSING, 	 4, CLOSING,		0",
        "4,	UNDERCUT, 	 4, UNDERCUT,		0",
        "4,	FINISHED, 	 4, FINISHED,		0",
    })
    @ParameterizedTest
    public void testNextWithoutSway(int startingRound, Phase startingPhase, int endingRound, Phase expectedPhase, int dbSaves) throws BadFormatException {
        // setup
        Tracker startingTracker = new Tracker(
                0,6, 				// the provider and the client start at opposite ends
                4,startingRound, 	// there are a max of 4 rounds
                0,0,				// both sides do not have any sway
                false, 				// not a secret
                startingPhase);
        db.put(CHANNEL_ID, startingTracker);

        // run
        cmd.nextRound(null, null, CHANNEL);

        // validate
        verify(storage, times(dbSaves)).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(expectedPhase, tracker.getPhase(),			"Confirm we 'nexted' into the correct phase.");
        assertEquals(endingRound, 	tracker.getCurrentRound(),	"Confirm we 'nexted' into the correct round");
        assertEquals(0, 			tracker.getProvider(), 		"Regardless of phase, the provider should not move since there was no sway");
        assertEquals(6, 			tracker.getClient(), 		"Regardless of phase, the client should not move since there was no sway");
        assertEquals(false, 		tracker.isSecret(),			"Make sure we didn't somehow flip the secret flag");

    }

    @CsvSource({
        "0,	 0,  0,  6",
        ",	 ,   0,  6",
        "1,	 0,  1,  6",
        "1,	  ,  1,  6",
        "1,	 1,  1,  5",
        ",	 1,  0,  5",
        "12, 0,  5,  6",
        "0,  12, 0,  1",
        "-2, -2, 0,  6",
        "5,  5,  2,  3",
        "6,  5,  3,  4",
        "5,  6,  1,  2",
        "12, 12, 2,  3",
    })
    @ParameterizedTest
    public void testNextWithSway(Integer providerSway, Integer clientSway, int expectedProvider, int expectedClient) throws BadFormatException {
        // setup
        Tracker startingTracker = new Tracker(
                0,6, 				// the provider and the client start at opposite ends
                4,1, 				// there are a max of 4 rounds, and we start at round 1
                0,0,				// both sides do not have any sway
                false, 				// not a secret
                Phase.NEGOTIATION);
        db.put(CHANNEL_ID, startingTracker);

        // run
        cmd.nextRound(providerSway, clientSway, CHANNEL);

        // validate
        verify(storage).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(Phase.NEGOTIATION, tracker.getPhase(),			"Confirm the phase remained the same since there are still rounds left");
        assertEquals(2, 				tracker.getCurrentRound(),	"Confirm we 'nexted' into round 2");
        assertEquals(expectedProvider, 	tracker.getProvider(), 		"Confirm the sway was applied to the provider correctly");
        assertEquals(expectedClient, 	tracker.getClient(), 		"Confirm the sway was applied to the client correctly");
        assertEquals(false, 			tracker.isSecret(),			"Make sure we didn't somehow flip the secret flag");

    }

    @CsvSource({
        "0,  0,  0,  0",
        ",   ,   0,  0",
        "1,  0,  1,  0",
        "1,   ,  1,  0",
        "1,  1,  1,  1",
        ",   1,  0,  1",
        "12, 0,  12, 0",
        "0,  12, 0,  12",
        "-2, -2, -2, -2",
        "5,  5,  5,  5",
        "6,  5,  6,  5",
        "5,  6,  5,  6",
        "12, 12, 12,  12",
    })
    @ParameterizedTest
    public void testSway(Integer providerSway, Integer clientSway, int expectedProvider, int expectedClient) throws BadFormatException {
        // setup
        Tracker startingTracker = new Tracker(
                0,6, 				// the provider and the client start at opposite ends
                4,1, 				// there are a max of 4 rounds, and we start at round 1
                0,0,				// both sides do not have any sway
                false, 				// not a secret
                Phase.NEGOTIATION);
        db.put(CHANNEL_ID, startingTracker);

        // run
        cmd.sway(providerSway, clientSway, CHANNEL);

        // validate
        verify(storage).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.size());

        Tracker tracker = db.values().iterator().next();
        assertEquals(Phase.NEGOTIATION, tracker.getPhase(),			"Confirm the phase remained the same");
        assertEquals(1, 				tracker.getCurrentRound(),	"Confirm we remained on the same round");
        assertEquals(expectedProvider,	tracker.getSwayProvider(), 	"Confirm the provider sway was stored");
        assertEquals(expectedClient, 	tracker.getSwayClient(), 	"Confirm the client sway was stored");
        assertEquals(0, 				tracker.getProvider(), 		"Confirm the actual provider was not moved");
        assertEquals(6, 				tracker.getClient(), 		"Confirm the actual client was not moved");
        assertEquals(false, 			tracker.isSecret(),			"Make sure we didn't somehow flip the secret flag");

    }

    @Test
    public void testSwayNoTracker() throws BadFormatException {
        // run
        String reply = cmd.sway(null, null, CHANNEL);

        // validate
        assertEquals(BEGIN_NEW_TRACKER, reply);
    }


    @ValueSource(strings = {"CLOSING", "UNDERCUT", "FINISHED"})
    @ParameterizedTest
    public void testSwayOutsideNegotations(Phase skippablePhases) throws BadFormatException {
        // setup
        // setup
        Tracker startingTracker = new Tracker(
                0,6, 				// the provider and the client start at opposite ends
                4,1, 				// there are a max of 4 rounds, and we start at round 1
                0,0,				// both sides do not have any sway
                false, 				// not a secret
                skippablePhases);
        db.put(CHANNEL_ID, startingTracker);

        // run
        cmd.sway(null, null, CHANNEL);

        // validate
        verify(storage, never()).storeTracker(eq(CHANNEL_ID), any());
        Tracker tracker = db.values().iterator().next();

        assertEquals(skippablePhases, 	tracker.getPhase(),			"Confirm the phase remained the same");
        assertEquals(1, 				tracker.getCurrentRound(),	"Confirm we remained on the same round");
        assertEquals(4, 				tracker.getTotalRounds(),	"Confirm total rounds never updated");
        assertEquals(0,					tracker.getSwayProvider(), 	"Confirm the provider sway remained the same");
        assertEquals(0, 				tracker.getSwayClient(), 	"Confirm the client sway remained the same");
        assertEquals(0, 				tracker.getProvider(), 		"Confirm the actual provider was not moved");
        assertEquals(6, 				tracker.getClient(), 		"Confirm the actual client was not moved");
        assertEquals(false, 			tracker.isSecret(),			"Make sure we didn't somehow flip the secret flag");

    }

}
