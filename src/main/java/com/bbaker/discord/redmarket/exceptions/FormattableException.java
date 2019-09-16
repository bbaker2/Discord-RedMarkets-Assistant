package com.bbaker.discord.redmarket.exceptions;

public class FormattableException extends Exception {

    public FormattableException(String msg, Object... args) {
        super(String.format(msg, args));
    }

}