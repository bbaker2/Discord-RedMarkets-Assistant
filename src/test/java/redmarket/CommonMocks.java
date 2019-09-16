package redmarket;

import org.javacord.api.entity.message.Message;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CommonMocks {

    public static final long USER_ID = 11111111;
    public static final long CHANNEL_ID = 22222222;

    public static Message genMsg(String content) {
        Message msg = mock(Message.class, Mockito.RETURNS_DEEP_STUBS);

        when(msg.getAuthor().getId()).thenReturn(USER_ID);
        when(msg.getChannel().getId()).thenReturn(CHANNEL_ID);
        when(msg.getContent()).thenReturn(content);

        return msg;
    }
}
