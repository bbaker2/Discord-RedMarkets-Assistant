package com.bbaker.discord.redmarket;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.javacord.api.AccountType;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import com.bbaker.discord.redmarket.commands.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.RedMarketCommand;
import com.bbaker.discord.redmarket.commands.RoleCommand;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;

public class Launcher {

    public static void main(String[] args) {
        String token = null;
        if(args.length < 1) {
            System.out.println("Missing token. Exiting");
            System.exit(1);
        } else {
            token = args[0];
        }

        DiscordApiBuilder dab = new DiscordApiBuilder().setAccountType(AccountType.BOT).setToken(token);
        
        try {
        	DiscordApi api = dab.login().join();
            api.setMessageCacheSize(1, 60);
            CommandHandler ch = new JavacordHandler(api);
            ch.registerCommand(new RedMarketCommand(ch));
            ch.registerCommand(new NegotiationCommand(api));
            ch.registerCommand(new RoleCommand());
        } catch (CancellationException | CompletionException e) {
            System.out.println("Ran into issues while connecting to discord");
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
        	e.printStackTrace();
        	System.exit(1);
        }        
    }

}
