package com.bbaker.discord.redmarket.commands.negotiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;

import com.bbaker.arg.parser.exception.BadArgumentException;
import com.bbaker.arg.parser.text.TextArgumentParser;
import com.bbaker.discord.redmarket.SwayProcessor;
import com.bbaker.discord.redmarket.Tracker;
import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.commands.roll.DiceRoller;
import com.bbaker.discord.redmarket.commands.roll.Table;
import com.bbaker.discord.redmarket.exceptions.BadFormatException;

import de.btobastian.sdcf4j.Command;

public class NegotiationCommand implements StandardCommand {

    private static final String CRIT = "Crit";
    private static final String SUCCESS = "Success";
    private static final String FAIL = "Fail";

    private Tracker tracker;
    private DiscordApi api;
    private TextArgumentParser parser;

    public NegotiationCommand(DiscordApi api) {
        this.api = api;
        this.parser = new TextArgumentParser();
    }

    @Command(aliases = {"!n", "!negotiate"}, description = "For planning stuff")
    public String negotiate(DiscordApi api, Message message) {
        List<String> args = getArgs(message);
        try {
            return evaluate(args);
        } catch (Throwable e) {
            return e.getMessage();
        }
    }

    private String evaluate(List<String> args) throws BadFormatException {
        switch(pop(args)) {
            case "start":
                return start(args);
            case "sway":
            case "s":
                return sway(args);
            case "round":
            case "r":
            case "next":
            case "n":
                return nextRound();
        }
        return appendStatus();

    }

    private String start(List<String> args) throws BadFormatException {
        System.out.println("Start!");
        DiceRoller dr = new DiceRoller();
        Iterator<String> tokens = args.iterator();
        while(tokens.hasNext()) {
            if(dr.processToken(tokens.next())) {
                tokens.remove();
            }
        }

        Table table = dr.getTable();
        tracker = new Tracker(table);

        StringBuilder sb = new StringBuilder();
        sb.append(table.getResults(api));
        if(table.isCrit()) {
            sb.append(" `").append(CRIT).append("`");
        }

        sb.append("\n");
        sb.append(appendStatus());

        return sb.toString();
    }

    private String sway(List<String> args) throws BadFormatException {
        try {
            parser.processArguments(args.iterator(), new SwayProcessor(tracker));
        } catch (BadArgumentException e) {
            throw new BadFormatException(e.getMessage());
        }

        return String.format("Provider Sway: `%d`; Client Sway: `%s`",
            tracker.getSwayProvider(),
            tracker.getSwayClient()
        );
    }

    private String nextRound() {

        if(tracker.getCurrentRound() < tracker.getTotalRounds()) {
            tracker.next();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(appendStatus());

        if(tracker.getCurrentRound() >= tracker.getTotalRounds()) {
            sb.append("\n");
            sb.append("Negotiations are over. Start closing phase.");
        }

        return sb.toString();
    }

    private String appendStatus() {
        String round = tracker.isSecret() ? "[SECRET]" : String.valueOf(tracker.getTotalRounds());
        String status = String.format("Round %d of %s. Provider: `%s`; Client: `%s`",
                tracker.getCurrentRound(),
                round,
                tracker.getProviderTrack(),
                tracker.getClientTrack());

        String[] emojies = new String[] {":heavy_minus_sign:", ":heavy_minus_sign:", ":heavy_minus_sign:"
                , ":heavy_minus_sign:", ":heavy_minus_sign:", ":heavy_minus_sign:", ":heavy_minus_sign:"};

        emojies[tracker.getClient()-1] = getDiceFace("red", tracker.getClient());
        emojies[tracker.getProvider()-1] = getDiceFace("black", tracker.getProvider());
        return status + "\n" + String.join(" ", emojies);
    }

    private String getDiceFace(String color, int face) {
        String name = String.format("%s_%02d", color, face);
        Collection<KnownCustomEmoji> emojies = api.getCustomEmojisByName(name);

        if(emojies.size() > 0) {
            return emojies.iterator().next().getMentionTag();
        } else {
            return color + ": " + face;
        }
    }

    public List<String> getArgs(Message message){
        String args = message.getContent();
        String[] argsArray = args.split("\\s+");
        List<String> argsList = new ArrayList(Arrays.asList(argsArray));
        argsList.remove(0);
        return argsList;
    }

    public <T> T pop(List<T> stack) {
        return stack.remove(0);
    }

}
