package redmarket.commands.negotiation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.javacord.api.DiscordApi;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.exceptions.verification.SmartNullPointerException;

import com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationStorage;
import com.bbaker.discord.redmarket.commands.negotiation.Tracker;

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


}
