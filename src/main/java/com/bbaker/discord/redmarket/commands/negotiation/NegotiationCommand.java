package com.bbaker.discord.redmarket.commands.negotiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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

    private static final String CRIT = "`Crit`";
    private static final String SUCCESS = "`Success`";
    private static final String FAIL = "`Fail`";
    private static final String DASH = "-"; //":heavy_minus_sign:";

    private NegotiationStorage storage;
    private DiscordApi api;
    private TextArgumentParser parser;

    public NegotiationCommand(DiscordApi api, NegotiationStorage storage) {
        this.api = api;
        this.storage = storage;
        this.parser = new TextArgumentParser();
    }

    @Command(aliases = {"!n", "!negotiate"}, description = "For planning stuff")
    public String negotiate(DiscordApi api, Message message) {
        try {
            return evaluate(message);
        } catch (Throwable e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private String evaluate(Message message) throws BadFormatException {
        List<String> args = getArgs(message);
        long channelId = message.getChannel().getId();

        if(args.size() == 0) {
            Optional<Tracker> tracker = storage.getTracker(channelId);
            if(tracker.isPresent()) {
                return appendStatus(tracker.get());
            } else {
                return start(args, channelId);
            }
        }

        String cmd = pop(args);
        switch(cmd) {
            case "start":
                return start(args, channelId);
            case "sway":
            case "s":
                return sway(args, channelId);
            case "round":
            case "r":
            case "next":
            case "n":
                return nextRound(channelId);
            default:
                return "Unknown argument `" + cmd + "`";
        }

    }

    private String start(List<String> args, long channelId) throws BadFormatException {
        DiceRoller dr = new DiceRoller();
        Iterator<String> tokens = args.iterator();
        while(tokens.hasNext()) {
            if(dr.processToken(tokens.next())) {
                tokens.remove();
            }
        }

        Table table = dr.getTable();
        Tracker tracker = new Tracker(table);
        storage.storeTracker(channelId, tracker);

        StringBuilder sb = new StringBuilder(table.getFullResults(api));

        sb.append("\n");

        if(table.isCrit()) {
            if(table.isSuccess()) {
                sb.append("The Providers get move up one sway automatically.");
            } else {
                sb.append("The Providers moved back one sway automatically.");
            }
            sb.append("\n");
        }

        if(table.isSuccess()) {
            sb.append(getDiceFace("black", table.getBlack()))
            .append("/2 rounded up equals ")
            .append(tracker.getTotalRounds()).append(" rounds");
        } else {
            sb.append("A random number of rounds was rolled in sceret for the Providers");
        }

        sb.append("\n");

        sb.append(appendStatus(tracker));

        return sb.toString();
    }

    private String sway(List<String> args, long channelId) throws BadFormatException {
        Tracker tracker = storage.getTracker(channelId).get();

        try {
            parser.processArguments(args.iterator(), new SwayProcessor(tracker));
        } catch (BadArgumentException e) {
            throw new BadFormatException(e.getMessage());
        }

        storage.storeTracker(channelId, tracker);
        return String.format("Provider Sway: `%d`; Client Sway: `%d`",
            tracker.getSwayProvider(),
            tracker.getSwayClient()
        );
    }

    private String nextRound(long channelId) {
        Tracker tracker = storage.getTracker(channelId).get();
        if(tracker.getCurrentRound() < tracker.getTotalRounds()) {
            tracker.next();
            storage.storeTracker(channelId, tracker);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(appendStatus(tracker));

        if(tracker.getCurrentRound() >= tracker.getTotalRounds()) {
            sb.append("\n");
            sb.append("Negotiations are over. Start closing phase.");
        }

        return sb.toString();
    }

    private String appendStatus(Tracker tracker) {
        String round = tracker.isSecret() ? "[SECRET]" : String.valueOf(tracker.getTotalRounds());
        String status = String.format("Round %d of %s. Provider: `%s`; Client: `%s`",
                tracker.getCurrentRound(),
                round,
                tracker.getProviderTrack(),
                tracker.getClientTrack());

        // prepare the default dashes
        String[] emojies = new String[] {DASH, DASH, DASH, DASH, DASH, DASH, DASH};

        // then truncate specific positions with the client and provider
        emojies[tracker.getClient()] = getDiceFace("red", tracker.getClient()+1);
        emojies[tracker.getProvider()] = getDiceFace("black", tracker.getProvider()+1);
        return status + "\n" + String.join(" ", emojies);
    }

    private String getDiceFace(String color, int face) {
        String name = String.format("%s_%02d", color, face);
        Collection<KnownCustomEmoji> emojies = api.getCustomEmojisByName(name);

        if(emojies.size() > 0) {
            return emojies.iterator().next().getMentionTag();
        } else {
            return color + "[" + face + "]";
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
