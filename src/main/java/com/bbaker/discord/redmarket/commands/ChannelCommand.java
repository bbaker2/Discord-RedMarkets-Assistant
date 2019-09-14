package com.bbaker.discord.redmarket.commands;

import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class ChannelCommand implements CommandExecutor {

    private DiscordApi api = null;
    private Optional<ChannelCategory> category = null;
    private Permissions playerPerms = null;
     
    
    public ChannelCommand(DiscordApi api, String category) {
        this.api = api;
        this.category = api.getChannelCategoriesByNameIgnoreCase(category)
        		.stream().findFirst();
        playerPerms = new PermissionsBuilder()
        		.setAllowed(
        				PermissionType.READ_MESSAGE_HISTORY,
        				PermissionType.READ_MESSAGES,
        				PermissionType.ADD_REACTIONS,
        				PermissionType.ATTACH_FILE,
        				PermissionType.SEND_MESSAGES,
        				PermissionType.SEND_TTS_MESSAGES,
        				PermissionType.USE_EXTERNAL_EMOJIS,
        				PermissionType.SPEAK,
        				PermissionType.EMBED_LINKS,
        				PermissionType.MENTION_EVERYONE
				)
        		.build();
    }

    @Command(aliases = {"!channel", "!c"}, description = "", usage = "!lft")
    public String onChannel(Message message) {
        String[] args = message.getContent().split("\\s+");
        if(args.length <= 2) {
            return "Missing action. Try `!channel add <chanel_name>`";
        }

        if(args.length < 3) {
            return "Missing channel name. Try `!channel add <chanel_name>`";
        }

        String channel = args[2];

        Server server = message.getServer().get();
        User creator = message.getUserAuthor().get();
        
        category.ifPresent(c -> {
            server.createTextChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(creator, playerPerms)
                .create();
            
            server.createVoiceChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(creator, playerPerms)
            	.create();
        });


        return "";
    }

}
