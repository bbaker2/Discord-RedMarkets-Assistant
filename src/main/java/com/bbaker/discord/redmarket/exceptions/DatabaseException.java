package com.bbaker.discord.redmarket.exceptions;

public class DatabaseException extends FormattableException {

	public DatabaseException(String msg, Object...args) {
		super(msg, args);
	}

}
