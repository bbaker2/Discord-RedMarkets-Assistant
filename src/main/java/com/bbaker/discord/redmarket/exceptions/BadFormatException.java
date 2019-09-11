package com.bbaker.discord.redmarket.exceptions;

public class BadFormatException extends Exception {
	
	public BadFormatException(String msg, Object... args) {
		this(String.format(msg, args));
	}
	
	public BadFormatException(String msg) {
		super(msg);
	}
}
