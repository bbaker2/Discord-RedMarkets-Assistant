package com.bbaker.discord.redmarket.exceptions;

public class BadFormatException extends FormattableException {

    public BadFormatException(String msg, Object... args) {
        super(msg, args);
    }

}
