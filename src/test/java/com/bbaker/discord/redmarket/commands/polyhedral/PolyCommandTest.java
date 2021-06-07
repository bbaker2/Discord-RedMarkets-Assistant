package com.bbaker.discord.redmarket.commands.polyhedral;

import static com.bbaker.discord.redmarket.commands.polyhedral.PolyCommand.*;
import static java.lang.String.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redmarket.CommonMocks;

class PolyCommandTest extends CommonMocks{

    private PolyCommand polyCommand;
    private DiscordApi api;

    @BeforeEach
    void setup() {
        polyCommand = new PolyCommand();
        api = mock(DiscordApi.class);
    }

    @Test
    void testPolyCommand() {
        //                        1 + 2 - 3 * 4 / 6
        Message msg = genMsg("!p d1 + 2d1 - 3d1 * 4d1 / 6d1", USER);

        String actual = polyCommand.onRoll(api, msg);
        String expected = format(RESULT_TEMPLATE,
                USER.getMentionTag(),
                1,
                "1 + [1, 1] - [1, 1, 1] * [1, 1, 1, 1] / [1, 1, 1, 1, 1, 1]");
        assertEquals(expected, actual, "Make ");
    }

    @Test
    void testBadInput() {
        Message msg = genMsg("!p words result in errors");
        String actual = polyCommand.onRoll(api, msg);
        String expected = format(BAD_INPUT_TEMPLATE, USER.getMentionTag());

        assertEquals(expected, actual, "Make sure invalid inputs return the correct message");

    }

}
