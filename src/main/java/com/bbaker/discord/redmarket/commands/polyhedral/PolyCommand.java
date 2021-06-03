package com.bbaker.discord.redmarket.commands.polyhedral;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;

import com.bbaker.discord.redmarket.commands.StandardCommand;

import de.btobastian.sdcf4j.Command;

public class PolyCommand implements StandardCommand {

    @Command(aliases 	= {"!p", "!poly", "!polyhedral"},
            description = "A generic dice roller",
            usage 		= "!p [times]d[size] example: 4d12 for a twelve sided die rolled four times")
    public String onRoll(DiscordApi api, Message message) {
        List<String> args = getArgs(message);

        return "`" + String.join("  ", args) + "`";
    }

}
