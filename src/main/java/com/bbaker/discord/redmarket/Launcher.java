package com.bbaker.discord.redmarket;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
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
        Properties props = new Properties();;
        if(args.length < 1) {
            System.out.println("Properties file missing. Exiting");
            System.exit(1);
        } else {
            try {
                props.load(new FileInputStream(args[0]));
            } catch (IOException e) {
                System.out.println("Unable to read properties file. Exiting");
                System.exit(1);
            }
        }

        if(!props.containsKey("auth.token")) {
            System.out.println("Auth token missing. Please populate auth.token in the properties file");
            System.exit(1);
        }

        DiscordApiBuilder dab = new DiscordApiBuilder().setAccountType(AccountType.BOT).setToken(props.getProperty("auth.token"));

        try {
            DiscordApi api = dab.login().join();
            api.setMessageCacheSize(1, 60);
            CommandHandler ch = new JavacordHandler(api);
            ch.registerCommand(new RedMarketCommand(ch));
            ch.registerCommand(new NegotiationCommand(api));
            ch.registerCommand(new RoleCommand(api));
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
