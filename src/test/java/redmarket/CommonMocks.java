package redmarket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.mockito.Mockito;



public class CommonMocks {


    public static final long USER_ID = 11111111;
    public static final User USER = mock(User.class);

    public static final Channel CHANNEL = mock(Channel.class);
    public static final long CHANNEL_ID = 22222222;

    public static final Server SERVER = mock(Server.class);
    public static final long SERVER_ID = 22222222;

    private static long uniqueIds = 1;

    public CommonMocks() {
        when(USER.getId()).thenReturn(USER_ID);
        when(USER.getNicknameMentionTag()).thenReturn("<"+USER_ID+">");
        when(CHANNEL.getId()).thenReturn(CHANNEL_ID);
        when(SERVER.getId()).thenReturn(SERVER_ID);
    }


    public static Message genMsg(String content, Nameable...mentioned) {
        List<User> taggedUsers = new ArrayList<User>();
        List<ServerTextChannel> taggedChannels = new ArrayList<ServerTextChannel>();

        for(Nameable de : mentioned) {
            if(de instanceof ServerTextChannel) {
                taggedChannels.add((ServerTextChannel) de);
            } else if (de instanceof User) {
                taggedUsers.add((User) de);
            }
        }

       return genMsg(content, taggedUsers, taggedChannels);
    }

    public static Message genMsg(String content, List<User> taggedUsers, List<ServerTextChannel> taggedChannels) {
        Message msg = mock(Message.class, Mockito.RETURNS_DEEP_STUBS);

        when(msg.getAuthor().getId()).thenReturn(USER_ID);
        when(msg.getUserAuthor()).thenReturn(Optional.of(USER));
        when(msg.getChannel().getId()).thenReturn(CHANNEL_ID);
        when(msg.getContent()).thenReturn(content);
        when(msg.getServer()).thenReturn(Optional.of(SERVER));
        when(msg.getMentionedChannels()).thenReturn(taggedChannels);
        when(msg.getMentionedUsers()).thenReturn(taggedUsers);
        return msg;
    }

    public User mockUser(String username) {
        User u = mock(User.class);
        when(u.getName()).thenReturn(username);
        when(u.getMentionTag()).thenReturn("<"+username+">");
        when(u.getId()).thenReturn(uniqueIds++);
        when(SERVER.getMembersByDisplayNameIgnoreCase(username)).thenReturn(Arrays.asList(u));
        return u;
    }
}
