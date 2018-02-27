package com.bbaker.discord.redmarket;

import de.btobastian.javacord.AccountType;
import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;

public class Launcher {

	public static void main(String[] args) {
		if(args.length < 1) {
			System.out.println("Missing token. Exiting");
			System.exit(1);
		}
		
		String token = args[0];
		DiscordApiBuilder dab = new DiscordApiBuilder().setAccountType(AccountType.BOT).setToken(token);
		DiscordApi api = dab.login().join();
		CommandHandler ch = new JavacordHandler(api);
		ch.registerCommand(new RedMarketCommand(ch));
	}

}
