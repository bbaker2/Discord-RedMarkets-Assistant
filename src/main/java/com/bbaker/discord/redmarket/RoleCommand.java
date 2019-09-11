package com.bbaker.discord.redmarket;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class RoleCommand implements CommandExecutor {
  

    @Command(aliases = {"!lfg"}, description = "Togglers your role if you are looking for a group to party with", usage = "!lft")
    public void onRoll(DiscordApi api, Message message) {
       message.getUserAuthor().ifPresent(user -> {    	   
    	   api.getRolesByName("lfg").stream().findFirst().ifPresent(role -> {
    		  
    		  boolean preExisting = role.getUsers().stream().filter(u -> user.getId() == u.getId()).findFirst().isPresent();

    		  if(preExisting) {    			  
    			  user.removeRole(role);
    		  } else {    			  
    			  user.addRole(role);
    		  }
    	   	});
    	   
       });
    }

}
