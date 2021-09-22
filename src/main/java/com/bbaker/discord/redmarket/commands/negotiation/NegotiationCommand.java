package com.bbaker.discord.redmarket.commands.negotiation;

import static com.bbaker.discord.redmarket.commands.negotiation.Phase.CLOSING;
import static com.bbaker.discord.redmarket.commands.negotiation.Phase.FINISHED;
import static com.bbaker.discord.redmarket.commands.negotiation.Phase.NEGOTIATION;
import static com.bbaker.discord.redmarket.commands.negotiation.Phase.UNDERCUT;
import static com.bbaker.discord.redmarket.commands.roll.RedMarketCommand.parseTable;
import static java.lang.Math.max;
import static org.javacord.api.interaction.SlashCommandOptionType.BOOLEAN;
import static org.javacord.api.interaction.SlashCommandOptionType.INTEGER;

import java.util.Collection;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.commands.roll.BlackOption;
import com.bbaker.discord.redmarket.commands.roll.ModOption;
import com.bbaker.discord.redmarket.commands.roll.RedOption;
import com.bbaker.discord.redmarket.commands.roll.Table;
import com.bbaker.discord.redmarket.exceptions.BadFormatException;
import com.bbaker.slashcord.handler.annotation.Slash;
import com.bbaker.slashcord.handler.annotation.SlashException;
import com.bbaker.slashcord.handler.annotation.SlashMeta;
import com.bbaker.slashcord.handler.annotation.SlashOption;
import com.bbaker.slashcord.structure.annotation.CommandDef;
import com.bbaker.slashcord.structure.annotation.OptionDef;
import com.bbaker.slashcord.structure.annotation.SubCommandDef;

@SubCommandDef(
    name = "negotiate",
    description = "To help with managing the negotation tracker",
    subs = {
        @CommandDef(
            name = "start",
            description = "Start or restart a new negotation tracker. This will trigger a leadership roll.",
            options = {
                @OptionDef(RedOption.class),
                @OptionDef(BlackOption.class),
                @OptionDef(ModOption.class)
            }
        ),
        @CommandDef(
            name = "status",
            description = "Output the current state of the negotation."
        ),
        @CommandDef(
            name = "sway",
            description = "Add (or remove) sway to the provider or client",
            options = {
                @OptionDef(
                    name = "provider",
                    description = "The amount of sway you wish to add to the Provider",
                    type = INTEGER,
                    required = false
                ),
                @OptionDef(
                    name = "client",
                    description = "The amount of sway you wish to add to the Client",
                    type = INTEGER,
                    required = false
                )
            }
        ),
        @CommandDef(
            name = "next",
            description = "Apply sways and move to the next round"
        ),
        @CommandDef(
            name = "close",
            description = "Apply the final Leadership check, and/or apply the undercut",
            options = {
                @OptionDef(RedOption.class),
                @OptionDef(BlackOption.class),
                @OptionDef(ModOption.class),
                @OptionDef(
                    name = "bust",
                    description = "Apply bust rules during closing. Defaults = false (Boon)",
                    type = BOOLEAN,
                    required = false
                )
            }
        )
    }
)
public class NegotiationCommand implements StandardCommand {

    private static final String DASH = ":white_square_button:";

    private static final String NL_BLOCK = "\n> ";
    private static final String BLOCK = "> ";

    private NegotiationStorage storage;
    private DiscordApi api;

    public NegotiationCommand(DiscordApi api, NegotiationStorage storage) {
        this.api = api;
        this.storage = storage;
    }

    @Slash(command = "negotiate", sub = "start")
    @SlashException(BadFormatException.class)
    public String start(
            @SlashOption("red") Integer red,
            @SlashOption("black") Integer black,
            @SlashOption("mod") Integer mod,
            TextChannel channel) throws BadFormatException {
        Table table = parseTable(red, black, mod);

        Tracker tracker = new Tracker(table);
        tracker.setPhase(Phase.NEGOTIATION);
        storage.storeTracker(channel.getId(), tracker);

        StringBuilder sb = new StringBuilder(BLOCK);
        sb.append("Performing opening `Leadership` check");
        sb.append(NL_BLOCK);
        sb.append(table.getFullResults(api));
        sb.append(NL_BLOCK);

        if(table.isCrit()) {
            if(table.isSuccess()) {
                sb.append("The Providers get move up one sway automatically.");
            } else {
                sb.append("The Providers moved back one sway automatically.");
            }
            sb.append(NL_BLOCK);
        }

        if(table.isSuccess()) {
            sb.append(getDiceFace("black", table.getBlack()))
            .append("/2 rounded up equals ")
            .append(tracker.getTotalRounds()).append(" rounds");
        } else {
            sb.append("A random number of rounds was rolled in sceret for the Provider");
        }

        sb.append(NL_BLOCK);

        sb.append(appendStatus(tracker));

        return sb.toString();
    }

    @Slash(command = "negotiate", sub = "status")
    @SlashException(BadFormatException.class)
    public String status(@SlashMeta TextChannel channel) {
        Tracker tracker = storage.getTracker(channel.getId()).get();
        StringBuilder sb = new StringBuilder(BLOCK);
        sb.append(appendStatus(tracker));
        sb.append(NL_BLOCK);
        if(tracker.getPhase() == NEGOTIATION) {
            sb.append(printSway(tracker));
            sb.append(NL_BLOCK);
        }
        sb.append(printNextStep(tracker.getPhase()));
        return sb.toString();
    }

    @Slash(command = "negotiate", sub = "sway")
    @SlashException(BadFormatException.class)
    public String sway(
            @SlashOption("provider") Integer provider,
            @SlashOption("client") Integer client,
            @SlashMeta TextChannel channel) throws BadFormatException {
        long channelId = channel.getId();
        Tracker tracker = storage.getTracker(channelId).get();

        if(tracker.getPhase() != NEGOTIATION) {
            return status(channel);
        }

        if(provider != null) tracker.swayProvider(provider);
        if(client != null) tracker.swayClient(client);

        storage.storeTracker(channelId, tracker);
        return printSway(tracker);
    }

    private String printSway(Tracker tracker) {
        return String.format("Provider Sway: `%d`; Client Sway: `%d`",
            tracker.getSwayProvider(),
            tracker.getSwayClient()
        );
    }

    @Slash(command = "negotiate", sub = "next")
    public String nextRound(@SlashMeta TextChannel channel) {
        long channelId = channel.getId();
        Tracker tracker = storage.getTracker(channelId).get();
        if(tracker.getPhase() == NEGOTIATION && tracker.getCurrentRound() <= tracker.getTotalRounds()) {
            tracker.next();
            if(tracker.getCurrentRound() >= tracker.getTotalRounds()) {
                tracker.setPhase(Phase.CLOSING);
            }
            storage.storeTracker(channelId, tracker);
        }

        StringBuilder sb = new StringBuilder(BLOCK);
        sb.append(appendStatus(tracker));
        sb.append(NL_BLOCK);
        sb.append(printNextStep(tracker.getPhase()));

        return sb.toString();
    }

    @Slash(command = "negotiate", sub = "close")
    @SlashException(BadFormatException.class)
    public String close(
         @SlashOption("red") Integer red,
         @SlashOption("black") Integer black,
         @SlashOption("mod") Integer mod,
         @SlashOption("bust") Boolean bust,
         TextChannel channel) throws BadFormatException {
        Tracker tracker = storage.getTracker(channel.getId()).get();

        Table table = parseTable(red, black, mod);


        if(tracker.getPhase() == FINISHED) {
            return status(channel);
        }

        StringBuilder sb = new StringBuilder(BLOCK);
        if(tracker.getPhase() == NEGOTIATION) {
            sb.append("Attempting to end Negotations early with an Intimidation check.");
            sb.append(NL_BLOCK);
            sb.append(table.getFullResults(api));
            sb.append(NL_BLOCK);
            if(table.isSuccess()) {
                tracker.setPhase(CLOSING);
            } else {
                sb.append("The Client was unphased. Negotations continue.");
                sb.append(NL_BLOCK);
                return sb.append(status(channel)).toString();
            }

        }

        if(tracker.getPhase() == CLOSING) {
            // apply leadership role
            bust =  bust != null && bust == true;
            performClosing(bust, tracker, table, sb);
            // transition to undercut
            tracker.setPhase(UNDERCUT);
        } else if(tracker.getPhase() == UNDERCUT) {
            performUndercut(tracker, table, sb);
            tracker.setPhase(FINISHED);
        }


        sb.append(NL_BLOCK);


        sb.append("Final price: ").append("`").append(tracker.getProviderTrack()).append("`");
        sb.append(NL_BLOCK);
        sb.append(printSwayTracker(tracker));

        if(tracker.getPhase() != FINISHED) {
            sb.append(NL_BLOCK);
            sb.append(printNextStep(tracker.getPhase()));
        }

        return sb.toString();
    }

    private void performUndercut(Tracker tracker, Table table, StringBuilder sb) {
        sb.append("Performing final `CHA` check");
        sb.append(NL_BLOCK);
        sb.append(table.getFullResults(api));
        sb.append(NL_BLOCK);
        // apply CHA role
        if(table.isSuccess()) {
            sb.append("The Client aggress that the clients are worth the closing price.");
        } else {
            sb.append("Another Provider undercuts you. The Client pushes down the price.");
            int closing = max(0, tracker.getClient() - 1); // reduce the client by 1 to a minimum of 0
            tracker.close(closing);
        }
    }

    private void performClosing(Boolean bust, Tracker tracker, Table table, StringBuilder sb) {
        // check if we need bust rules AND
        // there 1 or more spaces between the client and provider
        if(tracker.getClient() - tracker.getProvider() > 1 && bust) {
            sb.append("Bust rules invoked. The Client accepts The Provider's price AS IS");
            tracker.close(tracker.getClient());
        } else {
            sb.append("Performing final `Leadership` check");
            sb.append(NL_BLOCK);
            sb.append(table.getFullResults(api));
            if(table.isSuccess()){
                // the provider moves into the client's space
                tracker.close(tracker.getClient());
            } else {
                // the client moves into the provider's space
                tracker.close(tracker.getProvider());
            }
        }
    }

    private String printNextStep(Phase phase) {
        switch (phase) {
        case FINISHED:
            return "Negotations are over. `/negotation start` to being a new tracker.";
        case CLOSING:
            return "Negotations are over. `/negotation close` to perform a closing Leadership check.";
        case UNDERCUT:
            return "Negotations are over. `/negotation close` to perform a undercut CHA check (if needed)";
        case NEGOTIATION:
            return "Negotations are still under way. `/negotation sway` or `/negotation next`";
        }

        return "";
    }


    private String appendStatus(Tracker tracker) {
        String status;
        // If we are done....
        if(tracker.getPhase() != NEGOTIATION) {
            status = String.format("Finished all %d round(s). Provider: `%s`; Client: `%s`",
                    tracker.getTotalRounds(),
                    tracker.getProviderTrack(),
                    tracker.getClientTrack());
        // otherwise print the in-progress status
        } else {
            String round = tracker.isSecret() ? "[SECRET]" : String.valueOf(tracker.getTotalRounds());
            status = String.format("Round %d of %s. Provider: `%s`; Client: `%s`",
                    tracker.getCurrentRound(),
                    round,
                    tracker.getProviderTrack(),
                    tracker.getClientTrack());
        }

        String swayTracker = printSwayTracker(tracker);
        return status + NL_BLOCK + swayTracker;
    }

    private String printSwayTracker(Tracker tracker) {
        // prepare the default dashes
        String[] emojies = new String[] {DASH, DASH, DASH, DASH, DASH, DASH, DASH};

        // then truncate specific positions with the client and provider
        emojies[tracker.getClient()] = getDiceFace("red", tracker.getSwayClient());
        emojies[tracker.getProvider()] = getDiceFace("black", tracker.getSwayProvider());

        return String.join(" ", emojies);
    }

    private boolean withinRange(int val) {
        return val >= 0 && val <= 8;
    }

    private String getDiceFace(String color, int face) {
        String name;
        if(withinRange(face)) {
            name = String.format("%s_%02d", color, face);
        } else {
            name = color;
        }
        Collection<KnownCustomEmoji> emojies = api.getCustomEmojisByName(name);

        if(emojies.size() > 0) {
            return emojies.iterator().next().getMentionTag();
        } else {
            return color + "[" + face + "]";
        }
    }

}
