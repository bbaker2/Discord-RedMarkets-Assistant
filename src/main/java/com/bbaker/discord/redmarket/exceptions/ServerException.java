package com.bbaker.discord.redmarket.exceptions;

public class ServerException extends FormattableException {

    public ServerException(String msg, Object... args) {
        super(msg, args);
    }

}
