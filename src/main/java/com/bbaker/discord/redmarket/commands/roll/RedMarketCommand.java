package com.bbaker.discord.redmarket.commands.roll;

import java.util.Iterator;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.exceptions.BadFormatException;
import com.bbaker.slashcord.handler.annotation.Slash;
import com.bbaker.slashcord.handler.annotation.SlashMeta;
import com.bbaker.slashcord.handler.annotation.SlashOption;
import com.bbaker.slashcord.structure.annotation.CommandDef;
import com.bbaker.slashcord.structure.annotation.OptionDef;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandHandler;

@CommandDef(
    name = "roll",
    description = "Roll 2d10, finds the difference, and applys any (optional) modifiers",
    options = {
        @OptionDef(RedOption.class),
        @OptionDef(BlackOption.class),
        @OptionDef(ModOption.class)
    }
)
@CommandDef(
    name = "attack",
    description = "Determines hit/miss & damage. (Success == damage)",
    options = {
        @OptionDef(RedOption.class),
        @OptionDef(BlackOption.class),
        @OptionDef(ModOption.class)
    }
)
@CommandDef(
    name = "defend",
    description = "Determines hit/miss & damage. (Failure == damage)",
    options = {
        @OptionDef(RedOption.class),
        @OptionDef(BlackOption.class),
        @OptionDef(ModOption.class)
    }
)
@CommandDef(
    name = "damage",
    description = "Determines how much damage goes where. No modifiers.",
    options = {
        @OptionDef(RedOption.class),
        @OptionDef(BlackOption.class),
        @OptionDef(ModOption.class)
    }
)
public class RedMarketCommand implements StandardCommand {
    private static final String CRIT = "Crit";
    private static final String SUCCESS = "Success";
    private static final String FAIL = "Fail";

    private final CommandHandler commandHandler;

    public RedMarketCommand(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Command(aliases = {"!r", "!roll"}, description = "", usage = "!roll [+n|-n]")
    public String onRoll(DiscordApi api, Message message) {

        Table table = null;
        try {
            table = parseTable(message);
        }catch (BadFormatException e) {
            return e.getMessage();
        }

        String crit = table.isCrit() ? CRIT : "";
        String success = table.isSuccess() ? SUCCESS : FAIL;
        User author = message.getUserAuthor().get();
        return String.format("%s: %s `%s%s` Net: %d", author.getMentionTag(), table.getResults(api), crit, success, table.getNet());
    }

    @Slash( command = "roll" )
    public String onRoll(
            @SlashMeta DiscordApi api,
            @SlashOption("red") Integer red,
            @SlashOption("black") Integer black,
            @SlashOption("mod") Integer mod) {
        try {
            Table table = parseTable(red, black, mod);
            return showTableResult(table, mod != null, api).toString();
        } catch  (BadFormatException e) {
            return e.getMessage();
        }

    }

    private StringBuilder showTableResult(Table table, boolean includeMod, DiscordApi api) {
        StringBuilder sb = new StringBuilder();

        sb.append(table.getResults(api));

        if(includeMod) {
            sb.append(" ");
            if(table.getMod() >= 0) {
                sb.append("+");
            }
            sb.append(table.getMod());
        }

        sb.append(" `");
        if(table.isCrit()) {
            sb.append(CRIT);
            sb.append(" ");
        }

        if(table.isSuccess()) {
            sb.append(SUCCESS);
        } else {
            sb.append(FAIL);
        }

        sb.append("`");

        if(!table.isCrit()) {
            sb.append(" Net: ").append(table.getNet());
        }

        return sb;
    }

    @Command(aliases = {"!a", "!attack"}, description = "Determines hit/miss & damage. (Success == damage)", usage = "!attack [+n|-n]")
    public String onAttack(DiscordApi api, Message message) {
        return checkedDamage(api, message, true);
    }

    @Command(aliases = {"!d", "!defend"}, description = "Determines hit/miss & damage. (Failure == damage)", usage = "!defend [+n|-n]")
    public String onDefend(DiscordApi api, Message message) {
        return checkedDamage(api, message, false);
    }

    @Command(aliases = {"!dmg", "!damage"}, description = "Determines how much damage goes where. No modifiers.", usage = "!dmg")
    public String onDamange(DiscordApi api, Message message) {
        Table table = null;

        try {
            table = parseTable(message);
        } catch (BadFormatException e) {
            return e.getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(table.getResults(api));
        if(table.isCrit()) {
            sb.append(" `").append(CRIT).append("`");
        }
        sb.append("\n");
        sb.append( generateDamage(table.getBlack(), table.getRed(), table.isCrit()) );
        return sb.toString();
    }

    @Command(aliases = {"!test", "!t"}, description = "Shhh, ignore this.")
    public String onEmoji(DiscordApi api, Message message) {
        System.out.println(message.getContent());
        return "Hello World";
    }

    @Command(aliases = {"!h", "!help"}, description = "Help", usage = "!help")
    public String ohHelp(String[] arg) {
        StringBuilder sb = new StringBuilder("```xml");

        for (CommandHandler.SimpleCommand simpleCommand : commandHandler.getCommands()) {
            if (!simpleCommand.getCommandAnnotation().showInHelpPage()) {
                continue; // skip command
            }
            sb.append("\n");
            if (!simpleCommand.getCommandAnnotation().requiresMention()) {
                // the default prefix only works if the command does not require a mention
                sb.append(commandHandler.getDefaultPrefix());
            }
            String usage = simpleCommand.getCommandAnnotation().usage();
            if (usage.isEmpty()) { // no usage provided, using the first alias
                usage = simpleCommand.getCommandAnnotation().aliases()[0];
            }
            sb.append(usage);
            String description = simpleCommand.getCommandAnnotation().description();
            if (!description.equals("none")) {
                sb.append(" | ").append(description);
            }
        }

        sb.append("\n```");
        return sb.toString();

    }

    @Slash( command = "attack" )
    public String onAttack(
            @SlashMeta DiscordApi api,
            @SlashOption("red") Integer red,
            @SlashOption("black") Integer black,
            @SlashOption("mod") Integer mod) {
        try {
            Table table = parseTable(red, black, mod);
            StringBuilder sb = showTableResult(table, mod != null, api);
            if(table.isSuccess()) {
                sb.append(
                    generateDamage(table.getBlack(), table.getRed(), table.isCrit())
                );
            }
            return sb.toString();
        } catch  (BadFormatException e) {
            return e.getMessage();
        }

    }

    @Slash( command = "defend" )
    public String onDefend(
            @SlashMeta DiscordApi api,
            @SlashOption("red") Integer red,
            @SlashOption("black") Integer black,
            @SlashOption("mod") Integer mod) {
        try {
            Table table = parseTable(red, black, mod);
            StringBuilder sb = showTableResult(table, mod != null, api);
            if(!table.isSuccess()) {
                sb.append(
                    generateDamage(table.getBlack(), table.getRed(), table.isCrit())
                );
            }
            return sb.toString();
        } catch  (BadFormatException e) {
            return e.getMessage();
        }
    }

    @Slash( command = "damage" )
    public String onDamange(
            @SlashMeta DiscordApi api,
            @SlashOption("red") Integer red,
            @SlashOption("black") Integer black,
            @SlashOption("mod") Integer mod) {
        try {
            Table table = parseTable(red, black, mod);
            StringBuilder sb = showTableResult(table, mod != null, api);
            sb.append(
                generateDamage(table.getBlack(), table.getRed(), table.isCrit())
            );
            return sb.toString();
        } catch  (BadFormatException e) {
            return e.getMessage();
        }
    }

    private Table parseTable(Message message) throws BadFormatException {
        DiceRoller dr = new DiceRoller();
        Iterator<String> tokens = getArgs(message).iterator();
        while(tokens.hasNext()) {
            if(dr.processToken(tokens.next())) {
                tokens.remove();
            }
        }
        return dr.getTable();
    }

    private Table parseTable(Integer red, Integer black, Integer mod) throws BadFormatException {
        Table table = new Table(0);
        if(red != null) table.setRed(red);
        if(black != null) table.setBlack(black);
        if(mod != null) table.setMod(mod);
        return table;
    }

    private String checkedDamage(DiscordApi api, Message message, Boolean dmgOnSuccess) {
        try {
            Table table = parseTable(message);
            return checkDamange(api, table, dmgOnSuccess);
        }catch (BadFormatException e) {
            return e.getMessage();
        }

    }

    private String checkDamange(DiscordApi api, Table table, boolean dmgOnSuccess) {
        String crit = table.isCrit() ? CRIT : "";
        String success = table.isSuccess() ? SUCCESS : FAIL;

        StringBuilder sb = new StringBuilder();
        sb.append( String.format("%s `%s %s`", table.getResults(api), crit, success) );

        if(table.isSuccess() == dmgOnSuccess) {
            sb.append(generateDamage(table.getBlack(), table.getRed(), table.isCrit()));
        }

        return sb.toString();
    }

    private String generateDamage(int black, int red, boolean isCrit) {
        String location;
        switch(red) {
            case 1:
            case 2:
                location = "Right Leg";
                break;
            case 3:
            case 4:
                location = "Left Leg";
                break;
            case 5:
                location = "Right Arm";
                break;
            case 6:
                location = "Left Arm";
                break;
            case 7:
            case 8:
            case 9:
                location = "Torso";
                break;
            case 10:
                location = "Head";
                break;
            default:
                location = "Okay, you some how rolled a red d10 and got a number outside of 1-10. WTF?";
        }
        return String.format("\n**%d dmg** to the **%s**", isCrit ? black*2 : black, location);
    }
}
