package com.bbaker.discord.redmarket.exceptions;

public class SetupException extends FormattableException {

    public SetupException(String msg, Object... args) {
        super(String.format(msg, args));
    }


}
