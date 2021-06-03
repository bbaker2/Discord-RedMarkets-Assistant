package com.bbaker.discord.redmarket.commands.polyhedral;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bernardomg.tabletop.dice.history.RollHistory;
import com.bernardomg.tabletop.dice.interpreter.DiceInterpreter;
import com.bernardomg.tabletop.dice.interpreter.DiceRoller;
import com.bernardomg.tabletop.dice.notation.DiceNotationExpression;
import com.bernardomg.tabletop.dice.parser.DefaultDiceParser;
import com.bernardomg.tabletop.dice.parser.DiceParser;

import de.btobastian.sdcf4j.Command;

public class PolyCommand implements StandardCommand {

    @Command(aliases 	= {"!p", "!poly", "!polyhedral"},
            description = "A generic dice roller",
            usage 		= "!p [times]d[size] example: 4d12 for a twelve sided die rolled four times")
    public String onRoll(DiscordApi api, Message message) {
        List<String> args = getArgs(message);

        DiceParser parser = new DefaultDiceParser();
        DiceNotationExpression result = parser.parse(String.join("", args));

        DiceInterpreter<RollHistory> roller = new DiceRoller();
        RollHistory history = roller.transform(result);
        return String.format("`%d` = %s", history.getTotalRoll(), history.toString());
    }

}
