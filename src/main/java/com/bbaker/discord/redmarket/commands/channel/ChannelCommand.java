package com.bbaker.discord.redmarket.commands.channel;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import com.bbaker.discord.redmarket.commands.StandardCommand;
import com.bbaker.discord.redmarket.db.DatabaseService;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class ChannelCommand implements CommandExecutor, StandardCommand {

    private DiscordApi api = null;
    private Optional<ChannelCategory> category = null;
    private Permissions playerPerms = null;
    private Pattern VALID_NAME = Pattern.compile("[\\w|\\-]+");
    private ChannelStorage database;

    public ChannelCommand(DiscordApi api, String category, DatabaseService dbService) {
        this.api = api;
        this.database = new ChannelStorageImpl(dbService);
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
        startup();
    }

    public void startup() {
    	database.createTables();
    }

    @Override
    public void shutdown() {
    	// Nothing for shutdowns
    }


    @Command(aliases = {"!channel", "!c"}, description = "", usage = "!lft")
    public String onChannel(Message message) {
    	Server server = message.getServer().get();
    	User creator = message.getUserAuthor().get();
    	boolean isGm = creator.getRoles(server).stream().anyMatch(r -> r.getName().toLowerCase().equals("gm"));
    	if(!isGm) {
    		return "Only GMs can create channels & add users for games. Try `!gm` to give yourself the GM role";
    	}




        List<String> args = getArgs(message);
        pop(args); // pop the first argument, since it will always be !c

        if(args.size() == 0) {
            return "Missing action. Supported actions: `create`";
        }

        String action = args.remove(0);

        switch (action.toLowerCase()) {
        	case "create":
        		return createChannel(args, creator, server);
        	default:
        		return String.format("Unknown action `%s`. Supported actions: `create`", action);
        }
    }

	private String createChannel(List<String> args, User creator, Server server) {
		if(args.isEmpty()) {
            return "Missing channel name. Try `!channel create <chanel_name>`";
        }

        String channel = args.remove(0);

        if(!VALID_NAME.matcher(channel).matches()) {
        	return "Channel name can only contain alpha-numeric, underscores, and dashes";
        }

        Role everyone = server.getEveryoneRole();
        category.ifPresent(c -> {
            server.createTextChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(everyone, c.getOverwrittenPermissions(everyone))
            	.addPermissionOverwrite(creator, playerPerms)
                .create();

            server.createVoiceChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(everyone, c.getOverwrittenPermissions(everyone))
            	.addPermissionOverwrite(creator, playerPerms)
            	.create();
        });

        return String.format("Channel `%s` created.", channel);
	}

}
