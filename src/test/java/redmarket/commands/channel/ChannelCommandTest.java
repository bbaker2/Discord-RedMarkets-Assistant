package redmarket.commands.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.channel.ServerVoiceChannelUpdater;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
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
    private ServerTextChannel textChannel;
    private ServerVoiceChannel voiceChannel;
    private ServerTextChannelUpdater stcu;
    private ServerVoiceChannelUpdater svcu;

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

           when(USER.getRoles(SERVER)).thenReturn(Arrays.asList(gmRole)); // the user has the GM role for this test

        cmd = new ChannelCommand(api, CATEGORY, storage);

        // Text Channel Setup
        stcu = mock(ServerTextChannelUpdater.class);
        textChannel = mock(ServerTextChannel.class);
        when(textChannel.getName()).thenReturn("hello-world");
        when(textChannel.getId()).thenReturn(5l);
        when(textChannel.getMentionTag()).thenReturn("<5:hello-world>");
        when(textChannel.createUpdater()).thenReturn(stcu);

        // Voice Channel Setup
        svcu = mock(ServerVoiceChannelUpdater.class);
        voiceChannel = mock(ServerVoiceChannel.class);
        when(voiceChannel.getName()).thenReturn("hello-world");
        when(voiceChannel.getId()).thenReturn(6L);
        when(voiceChannel.createUpdater()).thenReturn(svcu);

    }

    @Test
    void testCreateChannelWithBadName() {
        String actual = null;

        actual = cmd.onChannel(genMsg("!c create !!!!!helloWorld"));
        assertEquals(ChannelCommand.MSG_BAD_CHAN_NAME, actual);

        actual = cmd.onChannel(genMsg("!c create hello#world"));
        assertEquals(ChannelCommand.MSG_BAD_CHAN_NAME, actual);

        actual = cmd.onChannel(genMsg("!c create \\\\hello"));
        assertEquals(ChannelCommand.MSG_BAD_CHAN_NAME, actual);

        verify(SERVER, never()).createTextChannelBuilder();
        verify(SERVER, never()).createVoiceChannelBuilder();
        verify(storage, never()).registerChannel(anyLong(), anyLong(), any());
    }

    @Test
    void testCreatePreExistingChannel() {
         when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel));

         String actual = cmd.onChannel(genMsg("!c create hello-world"));
         assertEquals(String.format(ChannelCommand.MSG_DUPLICATE_CHANNEL, "hello-world"), actual);
    }

    @Test
    void testCreateChannel() {
        // hello-world is our expected new channel names
        CompletableFuture<ServerTextChannel> cfstc = mock(CompletableFuture.class);
        ServerTextChannelBuilder stcb = mock(ServerTextChannelBuilder.class);

        when(SERVER.createTextChannelBuilder()).thenReturn(stcb);
        when(stcb.setCategory(channelCategory)).thenReturn(stcb);
        when(stcb.create()).thenReturn(cfstc);
        detailedMockAndVerify(stcb, "hello-world", 111111l, textChannel, cfstc);

        CompletableFuture<ServerVoiceChannel> cfsvc = mock(CompletableFuture.class);
        ServerVoiceChannelBuilder svcb = mock(ServerVoiceChannelBuilder.class);

        when(SERVER.createVoiceChannelBuilder()).thenReturn(svcb);
        when(svcb.setCategory(channelCategory)).thenReturn(svcb);
        when(svcb.create()).thenReturn(cfsvc);
        detailedMockAndVerify(svcb, "hello-world", 222222l, voiceChannel, cfsvc);

        when(channelCategory.getOverwrittenPermissions(any(Role.class))).thenReturn(mock(Permissions.class));
        when(USER.getRoles(SERVER)).thenReturn(Arrays.asList(gmRole)); // the user has the GM role for this test

        String actual = cmd.onChannel(genMsg("!c create hello-world"));

        verify(storage).registerChannel(eq(111111l), eq(USER_ID), any());
        verify(storage).registerChannel(eq(222222l), eq(USER_ID), any());
        assertEquals(String.format(cmd.MSG_CHANNEL_CREATED, "hello-world"), actual, "Display the success message for the text and voice channel to be created");
    }

    @Test
    void testDeleteChannel() {
        Message msg = genMsg("!c delete hello-world");

        // Create 10 channels (5 voice, 5 text). Put our expected channel somewhere in the middle
        // hello-world should have Ids 5 & 6
        String[] dummyNames = new String[] {"foo", "bar", "fizz", "bar"};
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

        foundChannels.add(5, textChannel);
        foundChannels.add(6, voiceChannel);

        when(channelCategory.getChannels()).thenReturn(foundChannels);
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.of(USER_ID));
        when(storage.getOwner(voiceChannel.getId())).thenReturn(Optional.of(USER_ID));

        String actual = cmd.onChannel(msg);

        assertEquals(
                String.format(ChannelCommand.MSG_CHANNEL_DELETED, USER.getNicknameMentionTag(), "hello-world"),
                actual,
                "Make sure the correct success message was returned");
        verify(storage).unregisterChannel(5l);
        verify(storage).unregisterChannel(6l);

    }

    @Test
    void testNotCreatorDeleteChannel() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel));
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.of(USER_ID+1)); // make sure you return a user ID other than the one who sent the message

        String actual = cmd.onChannel(genMsg("!c delete " + textChannel.getName()));
        assertEquals(
                String.format(ChannelCommand.MSG_NOT_OWNER, USER.getNicknameMentionTag(), textChannel.getName()),
                actual,
                "Make sure an error is returned when the non-owner tries to delete a channel");
        verify(storage, never()).unregisterChannel(anyLong());

    }

    @Test
    void testDeleteNoOwner() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel));
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.empty());

        String actual = cmd.onChannel(genMsg("!c delete "+textChannel.getName()));
        assertEquals(
                String.format(ChannelCommand.MSG_NO_OWNER, USER.getNicknameMentionTag(), textChannel.getName()),
                actual,
                "Make sure we short circuit when we cannot find the owener in the DB");
        verify(storage, never()).unregisterChannel(anyLong());
    }

    @Test
    void testDeleteChannelNotFound() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(voiceChannel));

        String actual = cmd.onChannel(genMsg("!c delete hello-ward"));
        assertEquals(
                String.format(ChannelCommand.MSG_CHANNEL_NOT_FOUND, USER.getNicknameMentionTag(), "hello-ward"),
                actual,
                "Ward != World. So no matching channels should have been found");
        verify(storage, never()).getOwner(anyLong());
        verify(storage, never()).unregisterChannel(anyLong());
    }

    @Test
    void testDeleteWithChannelTag() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel, voiceChannel));
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.of(USER_ID));
        when(storage.getOwner(voiceChannel.getId())).thenReturn(Optional.of(USER_ID));

        String actual = cmd.onChannel(genMsg("!c delete "+textChannel.getMentionTag(), textChannel));
        assertEquals(
                String.format(ChannelCommand.MSG_CHANNEL_DELETED, USER.getNicknameMentionTag(), textChannel.getMentionTag()),
                actual,
                "Make sure the correct channels were found for deletion");

        verify(storage).unregisterChannel(textChannel.getId());
        verify(storage).unregisterChannel(voiceChannel.getId());
    }

    @Test
    void testAddUsers() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel, voiceChannel));
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.of(USER_ID));
        when(storage.getOwner(voiceChannel.getId())).thenReturn(Optional.of(USER_ID));

        User playerA = mockUser("jack");
        User playerB = mockUser("jill");

        // We are adding one user via nick name an one
        String msg = String.format("!c add %s %s %s", textChannel.getMentionTag(), playerA.getName(), playerB.getMentionTag());
        String actual = cmd.onChannel(genMsg(msg, playerB, textChannel));
        String expected = String.format(ChannelCommand.MSG_USR_ADDED, String.join(",", playerA.getMentionTag(), playerB.getMentionTag()), textChannel.getMentionTag());
        assertEquals(expected, actual, "Make sure the correct add user success message is returned");

        verify(stcu).addPermissionOverwrite(argThat(u -> u.getId() == playerA.getId()), any());
        verify(stcu).addPermissionOverwrite(argThat(u -> u.getId() == playerB.getId()), any());

        verify(svcu).addPermissionOverwrite(argThat(u -> u.getId() == playerA.getId()), any());
        verify(svcu).addPermissionOverwrite(argThat(u -> u.getId() == playerB.getId()), any());
    }

    @Test
    void testAddUsersNotAsOwner() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel, voiceChannel));
        when(storage.getOwner(textChannel.getId())).thenReturn(Optional.of(USER_ID+1));		// make sure the user ID is different from the message user
        when(storage.getOwner(voiceChannel.getId())).thenReturn(Optional.of(USER_ID+1)); 	// make sure the user ID is different from the message user

        User bob = mockUser("bob");

        // We are adding one user via nick name an one
        String msg = String.format("!c add %s %s", textChannel.getName(), bob.getName());
        String actual = cmd.onChannel(genMsg(msg, bob));
        assertEquals(
                String.format(ChannelCommand.MSG_NOT_OWNER, USER.getNicknameMentionTag(), textChannel.getName()),
                actual,
                "Make sure we short circuit when we cannot find the owener in the DB while adding users");

        // Make sure we do not attempt to add users anyways
        verify(textChannel, never()).createUpdater();
        verify(voiceChannel, never()).createUpdater();
    }

    @Test
    void testAddUserDoesNotExist() {
        when(channelCategory.getChannels()).thenReturn(Arrays.asList(textChannel, voiceChannel));
        String dneUser = "dns";
        String msg = String.format("!c add %s %s", textChannel.getMentionTag(), dneUser);
        String actual = cmd.onChannel(genMsg(msg));
        String expected = String.format(ChannelCommand.MSG_USER_NOT_FOUND, dneUser);

        assertEquals(expected, actual, "Make sure the correct error message was returned when naming a user who does not exist");
        // Make sure we do not attempt to add users anyways
        verify(textChannel, never()).createUpdater();
        verify(voiceChannel, never()).createUpdater();
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
