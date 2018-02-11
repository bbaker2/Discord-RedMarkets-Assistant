package com.bbaker.discord.redmarket;

import java.util.List;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.emoji.CustomEmoji;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;

public class RedMarketCommand implements CommandExecutor {	
	
	private final CommandHandler commandHandler;

    public RedMarketCommand(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }
	
	@Command(aliases = {"!test", "!t"})
	public String onEmoji(DiscordApi api, Message message) {
		System.out.println(message.getContent());
		return "Hello world";
	}
	
	@Command(aliases = {"!r", "!roll"}, description = "Roll 2d10, finds the difference, and applys any (optional) modifiers", usage = "!roll [+n|-n]")
    public String onRoll(DiscordApi api, Message message) {
		String[] arg = message.getContent().split("\\s+");
		long mod = 0;
		if(arg.length >= 2) {
			try {
				mod = Long.valueOf(arg[1]);			
			} catch(NumberFormatException nfe) {
				return String.format("'%s' is not a integer", arg[1]);
			}			
		}
					
		int red = roll();
		int black = roll();
		
		if(red == black) {
			String crit = red % 2 == 0 ? "Pass" : "Fail";
			return String.format("%s %s `Crit: %s`", getDiceFace("black",  black, api), getDiceFace("red", red, api), crit);
		} else {			
			long roll = black - red + mod;
			return String.format("%s %s `Net: %d`", getDiceFace("black",  black, api), getDiceFace("red", red, api), roll);		
		}
		
		
    }
	
	@Command(aliases = {"!d", "!dmg", "!damage"}, description = "Determins how much damange goes where", usage = "!dmg [+n|-n]")
    public String onDamange(DiscordApi api, Message message) {
		String[] arg = message.getContent().split("\\s+");
		long mod = 0;
		if(arg.length >= 2) {
			try {
				mod = Long.valueOf(arg[1]);			
			} catch(NumberFormatException nfe) {
				return String.format("'%s' is not a integer", arg[1]);
			}			
		}
			
		
		int red = roll();
		int black = roll();
		long roll = black - red + mod;
		boolean isCrit = red==black;
		boolean isHit = roll > 0;
		StringBuilder sb = new StringBuilder();
		
		if(isCrit) {
			String crit = red % 2 == 0 ? "Success" : "Fail";
			sb.append( String.format("%s %s `Crit %s`", getDiceFace("black",  black, api), getDiceFace("red", red, api), crit) );
		} else {
			String success = isHit ? "Success" : "Fail";
			sb.append( String.format("%s %s `%s`(%d)", getDiceFace("black",  black, api), getDiceFace("red", red, api), success, roll) );
		}
		
		if(!isHit) {
			return sb.toString();
		}
		
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
		sb.append( String.format("\n**%d dmg** to the **%s**", isCrit ? black*2 : black, location) );
		return sb.toString();		
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
}
