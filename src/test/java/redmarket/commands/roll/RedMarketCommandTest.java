package redmarket.commands.roll;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javacord.api.DiscordApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbaker.discord.redmarket.commands.roll.RedMarketCommand;

import de.btobastian.sdcf4j.CommandHandler;

import redmarket.CommonMocks;

public class RedMarketCommandTest extends CommonMocks {
    
    private CommandHandler commandHandler = null;
    private DiscordApi api = null;
    private RedMarketCommand cmd = null;

    private Pattern validRollRgx;

    @BeforeEach
    public void setup() {
        commandHandler = mock(CommandHandler.class);
        api = mock(DiscordApi.class);
        cmd = new RedMarketCommand(commandHandler);

        validRollRgx = Pattern.compile(USER.getMentionTag() + ": black: \\d+ red: \\d+ \\`(Crit)?(Fail|Success)\\` Net: -?\\d+");
    }

    @Test
    void testOnRoll() {
        String actual = null;

        actual = cmd.onRoll(api, genMsg("!r 1b", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 2black", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 3r", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 4red", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 5b", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 6black", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 7r", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 8red", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 9b", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r 10black", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r r1", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r red2", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r b3", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r black4", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r r5", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r red6", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r b7", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r black8", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r r9", USER));
        assertTrue(isValidRollResult(actual));
        actual = cmd.onRoll(api, genMsg("!r red10", USER));
        assertTrue(isValidRollResult(actual));
    }

    private boolean isValidRollResult(String rollResult){
        Matcher matcher = validRollRgx.matcher(rollResult);

        System.out.println(USER.getMentionTag() + ": black: \\d+ red: \\d+ \\`(Crit)?(Fail|Success)\\` Net: -?\\d+");
        System.out.println(rollResult);

        return matcher.find();
    }
}
