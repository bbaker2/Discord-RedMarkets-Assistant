package com.bbaker.discord.redmarket;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.javacord.api.AccountType;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;

public class Launcher {

	public static void main(String[] args) {
		String token;
		if(args.length < 1) {
			token = "NDExMzUyOTQzNzYyMDc5NzQ0.DWKorQ.n6-tIo_jsp8MadME3McNbbzX9JE";
			//System.out.println("Missing token. Exiting");
			//System.exit(1);
		} else {
			token = args[0];
		}
		
		DiscordApiBuilder dab = new DiscordApiBuilder().setAccountType(AccountType.BOT).setToken(token);
		DiscordApi api = null;
		try {
			api = dab.login().join();
		} catch (CancellationException | CompletionException e) {
			System.out.println("Ran into issues while connecting to discord");
			System.out.println(e.getMessage());
			System.exit(1);
		}
		CommandHandler ch = new JavacordHandler(api);			
		ch.registerCommand(new RedMarketCommand(ch));
	}

}
