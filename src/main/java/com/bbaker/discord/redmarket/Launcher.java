package com.bbaker.discord.redmarket;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.AccountType;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.commands.channel.ChannelCommand;
import com.bbaker.discord.redmarket.commands.channel.ChannelStorageImpl;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationCommand;
import com.bbaker.discord.redmarket.commands.negotiation.NegotiationStorageImpl;
import com.bbaker.discord.redmarket.commands.polyhedral.PolyCommand;
import com.bbaker.discord.redmarket.commands.role.RoleCommand;
import com.bbaker.discord.redmarket.commands.roll.RedMarketCommand;
import com.bbaker.discord.redmarket.db.DatabaseService;
import com.bbaker.discord.redmarket.db.DatabaseServiceImpl;
import com.bbaker.discord.redmarket.exceptions.SetupException;
import com.bbaker.slashcord.dispatcher.SlashCommandDispatcher;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;

public class Launcher {

    private static final Logger logger = LogManager.getLogger(Launcher.class);

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
            DatabaseService dbService = new DatabaseServiceImpl(props);
            DiscordApi api = dab.login().join();
            api.setMessageCacheSize(1, 60);
            CommandHandler ch = new JavacordHandler(api);

            List<StandardCommand> cmdList = Arrays.asList(
                new RedMarketCommand(ch),
                new NegotiationCommand(api, new NegotiationStorageImpl(dbService)),
                new RoleCommand(api),
                new ChannelCommand(api, props.getProperty("config.category"), new ChannelStorageImpl(dbService)),
                new PolyCommand()
            );
            SlashCommandDispatcher dispatcher = new SlashCommandDispatcher(api);

            for(StandardCommand sd : cmdList) {
                sd.startup();
                ch.registerCommand(sd);
                dispatcher.queue(sd);
            }

            dispatcher.submit().join().stream().forEach(System.out::println);
        } catch (CancellationException | CompletionException e) {
            logger.error("Ran into issues while connecting to discord", e);
            System.exit(1);
        } catch (SetupException e) {
            logger.error("Ran into an issue while starting up the database", e);
        } catch (Exception e) {
            logger.error(e);
            System.exit(1);
        }
    }

}
