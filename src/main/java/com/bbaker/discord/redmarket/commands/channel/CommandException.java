package com.bbaker.discord.redmarket.commands.channel;

import com.bbaker.discord.redmarket.exceptions.FormattableException;

public class CommandException extends FormattableException {

    public CommandException(String msg, Object... args) {
        super(msg, args);
    }

}
