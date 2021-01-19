package redmarket;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.mockito.Mockito;

import com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationStorage;
import com.bbaker.discord.redmarket.commands.negotiation.Tracker;

public class CommandSimulator extends CommonMocks {

     private static final Logger logger = LogManager.getLogger(CommandSimulator.class);

    public static void main(String...args) {
        DiscordApi api = mockApi();
        NegotiationStorage storage = mockStorage();

        NegotiationCommand cmd = new NegotiationCommand(api, storage);
        Scanner sc = new Scanner(System.in);
        for(String val = ""; !"exit".equalsIgnoreCase(val); val = sc.nextLine()) {
            Message msg = genMsg("!n " + val);
            String response = cmd.negotiate(api, msg);
            logger.error(response);
        }
        System.out.println("Exiting");

    }

    private static NegotiationStorage mockStorage() {
        return new NegotiationStorage() {

            Map<Long, Tracker> db = new HashMap<>();

            @Override
            public void storeTracker(long channelId, Tracker tracker) {
                db.put(channelId, tracker);
            }

            @Override
            public Optional<Tracker> getTracker(long channelId) {
                return db.containsKey(channelId) ? Optional.of(db.get(channelId)) : Optional.empty();
            }

            @Override
            public void createTable() {
                // TODO Auto-generated method stub

            }
        };
    }

    private static DiscordApi mockApi() {
        DiscordApi api = mock(DiscordApi.class);

        when(api.getCustomEmojiById(Mockito.anyString()))
        .thenAnswer(i ->
            Arrays.asList(
                when(mock(KnownCustomEmoji.class).getMentionTag())
                .thenReturn(i.getArgument(0, String.class)).getMock()
            )
        );
        return api;
    }
}
