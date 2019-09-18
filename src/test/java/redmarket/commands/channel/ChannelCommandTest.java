package redmarket.commands.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerChannelBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbaker.discord.redmarket.commands.channel.ChannelCommand;
import com.bbaker.discord.redmarket.commands.channel.ChannelStorage;

import redmarket.CommonMocks;

class ChannelCommandTest extends CommonMocks {


    private ChannelStorage storage = null;
    private DiscordApi api = null;
    private ChannelCategory channelCategory = null;
    private Role gmRole = null;
    private Role everyone = null;
    private ChannelCommand cmd = null;

    private static final String CATEGORY = "test";


    @BeforeEach
    public void setup() {
        storage = mock(ChannelStorage.class);
        api = mock(DiscordApi.class);
        channelCategory = mock(ChannelCategory.class);
        gmRole = mock(Role.class);
        everyone = mock(Role.class);

        when(gmRole.getName()).thenReturn("gm");

        when(everyone.getName()).thenReturn("@everyone");
        when(everyone.getId()).thenReturn(55555555l);

        when(api.getChannelCategoriesByNameIgnoreCase(CATEGORY)).thenReturn(Arrays.asList(channelCategory));

        when(SERVER.getEveryoneRole()).thenReturn(everyone);



        cmd = new ChannelCommand(api, CATEGORY, storage);

    }

    @Test
    void testCreateChannel() {
        // hello-world is our expected new channel names

        ServerTextChannel stc = mock(ServerTextChannel.class);
        CompletableFuture<ServerTextChannel> cfstc = mock(CompletableFuture.class);
        ServerTextChannelBuilder stcb = mock(ServerTextChannelBuilder.class);

        when(SERVER.createTextChannelBuilder()).thenReturn(stcb);
        when(stcb.setCategory(channelCategory)).thenReturn(stcb);
        when(stcb.create()).thenReturn(cfstc);
        detailedMockAndVerify(stcb, "hello-world", 111111l, stc, cfstc);

        ServerVoiceChannel svc = mock(ServerVoiceChannel.class);
        CompletableFuture<ServerVoiceChannel> cfsvc = mock(CompletableFuture.class);
        ServerVoiceChannelBuilder svcb = mock(ServerVoiceChannelBuilder.class);

        when(SERVER.createVoiceChannelBuilder()).thenReturn(svcb);
        when(svcb.setCategory(channelCategory)).thenReturn(svcb);
        when(svcb.create()).thenReturn(cfsvc);
        detailedMockAndVerify(svcb, "hello-world", 222222l, svc, cfsvc);



        when(channelCategory.getOverwrittenPermissions(any(Role.class))).thenReturn(mock(Permissions.class));
        when(USER.getRoles(SERVER)).thenReturn(Arrays.asList(gmRole)); // the user has the GM role for this test

        String actual = cmd.onChannel(genMsg("!c create hello-world"));

        verify(storage).registerChannel(eq(111111l), eq(USER_ID), any());
        verify(storage).registerChannel(eq(222222l), eq(USER_ID), any());
        assertEquals(String.format(cmd.MSG_CHANNEL_CREATED, "hello-world"), actual, "Display the success message for the text and voice channel to be created");
    }

    @Test
    void testRemoveChannel() {
        Message msg = genMsg("!c delete hello-world");
        when(msg.getMentionedChannels()).thenReturn(Arrays.asList()); // no tags were used for this test
        when(USER.getRoles(SERVER)).thenReturn(Arrays.asList(gmRole)); // the user has the GM role for this test

        // Create 10 channels (5 voice, 5 text). Put our expected channel somewhere in the middle
        // hello-world should have Ids 5 & 6
        String[] dummyNames = new String[] {"foo", "bar", "hello-world", "fizz", "bar"};
        List<ServerChannel> foundChannels = new ArrayList<ServerChannel>();
        long id = 1;
        for(String name : dummyNames) {
            ServerTextChannel stc = mock(ServerTextChannel.class);
            when(stc.getName()).thenReturn(name);
            when(stc.getId()).thenReturn(id++);
            foundChannels.add(stc);

            ServerVoiceChannel svc = mock(ServerVoiceChannel.class);
            when(svc.getName()).thenReturn(name);
            when(svc.getId()).thenReturn(id++);
            foundChannels.add(svc);
        }

        when(channelCategory.getChannels()).thenReturn(foundChannels);
        when(storage.getOwner(5l)).thenReturn(Optional.of(USER_ID));
        when(storage.getOwner(6l)).thenReturn(Optional.of(USER_ID));

        String actual = cmd.onChannel(msg);

        assertEquals(
                String.format(ChannelCommand.MSG_CHANNEL_DELETED, USER.getNicknameMentionTag(), "hello-world"),
                actual,
                "Make sure the correct success message was returned");
        verify(storage).unregisterChannel(5l);
        verify(storage).unregisterChannel(6l);

    }

    private <T extends ServerChannel> void detailedMockAndVerify(ServerChannelBuilder scb,  String name, long channelId, T serverChannel, CompletableFuture<T> future) {

        when(scb.setName(name)).thenReturn(scb);
        when(scb.addPermissionOverwrite(argThat(r -> r != null && r.getId() == everyone.getId()), any())).thenReturn(scb);
        when(scb.addPermissionOverwrite(argThat(u -> u != null && u.getId() == USER_ID), any())).thenReturn(scb);
        when(serverChannel.getId()).thenReturn(channelId);

        when(future.thenAccept(any())).thenAnswer(i -> {
            Consumer<T> action = i.getArgument(0);
            action.accept(serverChannel);
            return future;
        });
    }


}
