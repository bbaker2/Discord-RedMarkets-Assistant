package com.bbaker.discord.redmarket.commands.roll;

import java.util.Iterator;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.exceptions.BadFormatException;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandHandler;

public class RedMarketCommand implements StandardCommand {
    private static final String CRIT = "Crit";
    private static final String SUCCESS = "Success";
    private static final String FAIL = "Fail";

    private final CommandHandler commandHandler;

    public RedMarketCommand(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Command(aliases = {"!r", "!roll"}, description = "Roll 2d10, finds the difference, and applys any (optional) modifiers", usage = "!roll [+n|-n]")
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

    private String checkedDamage(DiscordApi api, Message message, Boolean dmgOnSuccess) {
        Table table;
        try {
            table = parseTable(message);
        }catch (BadFormatException e) {
            return e.getMessage();
        }

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
