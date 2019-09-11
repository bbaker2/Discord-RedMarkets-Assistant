package com.bbaker.discord.redmarket.commands;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class RoleCommand implements CommandExecutor {
  
	private DiscordApi api = null;	
			
	public RoleCommand(DiscordApi api) {
		this.api = api;
	}

    @Command(aliases = {"!lfg"}, description = "Togglers your role if you are looking for a group to party with", usage = "!lft")
    public void onFLG(Message message) {
       message.getUserAuthor().ifPresent(user -> toggleRole(user, "lfg"));
    }
    
    @Command(aliases = {"!gm"}, description = "Togglers your role if you want to declare yourself a Game Master", usage = "!gm")
    public void onGM(Message message) {
       message.getUserAuthor().ifPresent(user -> toggleRole(user, "gm"));
    }
    
    private void toggleRole(User user, String roleName) {
    	api.getRolesByName(roleName).stream().findFirst().ifPresent(role -> {
    		boolean preExisting = role.getUsers().stream().filter(u -> user.getId() == u.getId()).findFirst().isPresent();
    		if(preExisting) {    			  
    			user.removeRole(role);
    		} else {    			  
    			user.addRole(role);
    		}	   	
	    });
    }
}
