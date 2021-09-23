package redmarket.commands.negotiation;

import static com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        cmd.start(4,6,3, CHANNEL);

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
        cmd.start(4,4,3, CHANNEL);

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
        cmd.start(5,5,3, CHANNEL);

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
        String reply = cmd.status(CHANNEL);

        assertEquals(BEGIN_NEW_TRACKER, reply);
    }

    @Test
    public void testStatus() {
        Tracker tracker = new Tracker(2, 5, 4, 2, 1, 1, false, Phase.NEGOTIATION);
        db.put(CHANNEL_ID, tracker);

        String reply = cmd.status(CHANNEL);

        assertNotEquals(BEGIN_NEW_TRACKER, reply);
    }

    @Test
    public void testTracklessNext() throws BadFormatException {
        String reply = cmd.nextRound(1, 1, CHANNEL);

        assertEquals(BEGIN_NEW_TRACKER, reply);
    }

    public void testNext(int round, Phase startingPhase, Phase expectedPhase) throws BadFormatException {
        Tracker tracker = new Tracker(0,7,4,round,1,1,false, startingPhase);
        db.put(CHANNEL_ID, tracker);

        cmd.nextRound(null, null, CHANNEL);

    }


}
