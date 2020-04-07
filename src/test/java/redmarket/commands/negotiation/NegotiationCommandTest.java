package redmarket.commands.negotiation;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.javacord.api.DiscordApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.mockito.exceptions.verification.SmartNullPointerException;

import com.bbaker.discord.redmarket.Tracker;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationStorage;

import redmarket.CommonMocks;

class NegotiationCommandTest extends CommonMocks {
    private DiscordApi api;
    private NegotiationStorage storage;
    private NegotiationCommand cmd;

    public Map<Long, Tracker> db;

    @BeforeEach
    void setup() {
        api = mock(DiscordApi.class);
        storage = mock(NegotiationStorage.class);
        cmd = new NegotiationCommand(api, storage);

        db = new HashMap<Long, Tracker>();
        doAnswer(i->{
            long key = i.getArgument(0, long.class);
            Tracker val = i.getArgument(1, Tracker.class);
            db.put(key, val);
            return null;
        }).when(storage).storeTracker(anyLong(), any(Tracker.class));

        when(storage.getTracker(anyLong())).thenAnswer(i -> {
            long key = i.getArgument(0, long.class);
            if(db.containsKey(key)) {
                return db.get(key);
            }
            return new SmartNullPointerException(key + " does not have a tracker associated");
        });
    }

    @Test
    void testEmptyCommand() {
        cmd.negotiate(api, genMsg("!n"));
        cmd.negotiate(api, genMsg("!n"));
        verify(storage, times(2).description("Make sure we checked for pre-existing trackers twice")).getTracker(CHANNEL_ID);
        verify(storage, description("Make sure the correct channel id is used when storing")).storeTracker(eq(CHANNEL_ID), any());
        assertEquals(1, db.get(CHANNEL_ID).getCurrentRound(), "By default, start at round 1");
    }

    @TestTemplate
    void testStarts(String diceArgs, List<String> expectedResponses, Condition<Tracker> condition) {
        String[] actual = cmd.negotiate(api, genMsg("!n "+diceArgs)).split("\n");

        assertThat(actual).containsAll(expectedResponses).describedAs("The response must contain at least these responses");
        assertThat(db).hasEntrySatisfying(CHANNEL_ID, condition);
    }

    @Test
    void testEmptyNextEmpty() {
        cmd.negotiate(api, genMsg("!n n"));
    }

}
