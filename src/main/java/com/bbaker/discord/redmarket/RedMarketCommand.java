package com.bbaker.discord.redmarket;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.emoji.CustomEmoji;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;

public class RedMarketCommand implements CommandExecutor {	
	private static final String CRIT = "Crit";
	private static final String SUCCESS = "Success";
	private static final String FAIL = "Fail";
	private static final String[] DM = {
		"Nope", "No", "Not Allowed", "Normally, yes. But not this time", "Normally yes. But not for you", "Haha, no", "Never",
		"Maybe", "Sometimes", "Ask again?", "For 20 bucks, yes.",
		"Sure", "Yes", "Uhhhhhh, yes", "Valid", "This time, yes", "Normally no. But for you, yes.", "I guess so", "Yep"		
	};
	
	private final Pattern diceFaceRgx;
	private final Pattern modRgx;
			
	private final CommandHandler commandHandler;	
    
	public RedMarketCommand(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
        diceFaceRgx = Pattern.compile("(black|b|red|r)(\\d+)");
        modRgx = Pattern.compile("\\s((\\+?|-?)\\d+)(\\s|$)");
    }
	
	@Command(aliases = {"!r", "!roll"}, description = "Roll 2d10, finds the difference, and applys any (optional) modifiers", usage = "!roll [+n|-n]")
	public String onRoll(DiscordApi api, Message message) {
		Table table;
		try {
			long mod = getMod(message);
			table = new Table(mod).roll();
			updateTable(message, table);
		}catch (BadFormatException e) {
			return e.getMessage();
		}
		
		String crit = table.isCrit() ? CRIT : "";
		String success = table.isSuccess() ? SUCCESS : FAIL;
		return String.format("%s `%s%s` Net: %d", table.getResults(api), crit, success, table.getNet());
	}
	
	@Command(aliases = {"!a", "!attack"}, description = "Determins hit/miss & damage. (Success == damage)", usage = "!attack [+n|-n]")
	public String onAttack(DiscordApi api, Message message) {
		return checkedDamage(api, message, true);
	}
	
	@Command(aliases = {"!d", "!defend"}, description = "Determins hit/miss & damage. (Failure == damage)", usage = "!defend [+n|-n]")
	public String onDefend(DiscordApi api, Message message) {
		return checkedDamage(api, message, false);
	}
	
	@Command(aliases = {"!dmg", "!damage"}, description = "Determins how much damange goes where. No modifiers.", usage = "!dmg")
	public String onDamange(DiscordApi api, Message message) {
		Table table = new Table().roll();
		
		try {
			updateTable(message, table);
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
	
	@Command(aliases = {"!gm"}, description = "Returns a yes, no, or maybe at random")
	public String onGmSays(String[] args) {		
		int index = (int)Math.rint(Math.random() * DM.length);
		return DM[index];
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

	private String checkedDamage(DiscordApi api, Message message, Boolean dmgOnSuccess) {
		Table table;
		try {
			long mod = getMod(message);
			table = new Table(mod).roll();
			updateTable(message, table);
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
	
	
	private int roll() {
		return (int) (Math.random() * 10) + 1;
	}
	
	private String getDiceFace(String color, int face, DiscordApi api) {
		String name = String.format("%s_%02d", color, face);
		List<CustomEmoji> emojies = api.getCustomEmojisByName(name);
	
		if(emojies.size() > 0) {
			return emojies.get(0).getMentionTag();
		} else {
			return color + ": " + face;
		}
	}
	
	private long getMod(Message message) throws BadFormatException {
		
		Matcher m = modRgx.matcher(message.getContent());
		if(m.find()) {
			try {
				System.out.println("mod: " + m.group(1));
				return Long.valueOf(m.group(1));
			} catch(NumberFormatException nfe) {
				throw new BadFormatException(String.format("'%s' is not a integer", m.group(1)));
			}	
		} else {
			return 0;
		}
	}
	
	private void updateTable(Message message, Table table) throws BadFormatException {
		Matcher m = diceFaceRgx.matcher(message.getContent().toLowerCase());		
		while(m.find()) {
			System.out.println(m.group(1) + "--> " + m.group(2));
			switch(m.group(1)) {
				case "r":
				case "red":
					table.setRed(getInt(m.group(2)));
					break;
				case "b":
				case "black":
					table.setBlack(getInt(m.group(2)));
					break;
			}
		}
		
	}
	
	private int getInt(String val) throws BadFormatException {
		try {
			return Integer.valueOf(val);
		}catch (NumberFormatException e) {
			throw new BadFormatException("`%s` is not an integer.", val);
		}
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
