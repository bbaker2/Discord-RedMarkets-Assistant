package com.bbaker.discord.redmarket.commands.polyhedral;

import static java.lang.String.*;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bernardomg.tabletop.dice.history.RollHistory;
import com.bernardomg.tabletop.dice.interpreter.DiceInterpreter;
import com.bernardomg.tabletop.dice.interpreter.DiceRoller;
import com.bernardomg.tabletop.dice.notation.DiceNotationExpression;
import com.bernardomg.tabletop.dice.parser.DefaultDiceParser;

import de.btobastian.sdcf4j.Command;

public class PolyCommand implements StandardCommand {

    public static final String RESULT_TEMPLATE      = "%s: `%d` = %s";
    public static final String BAD_INPUT_TEMPLATE   = "%s: Unable to parse.";

    private DefaultDiceParser parser = new DefaultDiceParser();
    private DiceInterpreter<RollHistory> roller = new DiceRoller();

    @Command(aliases 	= {"!p", "!poly", "!polyhedral"},
            description = "A generic dice roller",
            usage 		= "!p [times]d[size] example: 4d12 for a twelve sided die rolled four times")
    public String onRoll(DiscordApi api, Message message) {
        List<String> args = getArgs(message);
        User author = message.getUserAuthor().get();
        try {
            DiceNotationExpression result = parser.parse(String.join("", args));
            RollHistory history = roller.transform(result);
            return format(RESULT_TEMPLATE, author.getMentionTag(), history.getTotalRoll(), history.toString());
        } catch (Exception e) {
            return format(BAD_INPUT_TEMPLATE, author.getMentionTag());
        }
    }

}
