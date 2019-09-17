package com.bbaker.discord.redmarket.commands.channel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
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

    public static final String MSG_CHANNEL_CREATED = "Channel `%s` created.";

	private DiscordApi api = null;
    private Permissions playerPerms = null;
    private String category = null;
    private Pattern VALID_NAME = Pattern.compile("[\\w|\\-]+");
    private ChannelStorage database;

    public ChannelCommand(DiscordApi api, String category, DatabaseService dbService) {
        this.api = api;
        this.category = category;
        this.database = new ChannelStorageImpl(dbService);
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

        if(args.size() == 0) {
            return "Missing action. Supported actions: `create`";
        }


        String action = args.remove(0);

        switch (action.toLowerCase()) {
        	case "create":
        		return createChannel(args, creator, server);
        	case "remove":
        		return removeChannel(args, creator, message.getMentionedChannels(), server);
        	case "add":
        		return addUser(args, creator, server);
        	default:
        		return String.format("Unknown action `%s`. Supported actions: `create`", action);
        }
    }

    private String addUser(List<String> args, User creator, Server server) {
		// TODO Auto-generated method stub
		return null;
	}

	private String removeChannel(List<String> args, User owner, List<ServerTextChannel> mentioned, Server server) {
		if(args.isEmpty()) {
            return "Missing channel name. Try `!channel remove <chanel_name>`";
        }

		String channel = pop(args);
		System.out.println(channel);

		ChannelCategory category = getCategory().get();

		// If someone used a #mentioned tag, attempt to convert it into this string name instead
		for(ServerTextChannel stc : mentioned) {
			if(stc.getMentionTag().contentEquals(channel)) {
				channel = stc.getName();
				break;
			}
		}

        List<ServerChannel> channelList = category.getChannels();
        List<String> responses = new ArrayList<String>();
        for(ServerChannel sc : channelList) {
        	if(sc.getName().equalsIgnoreCase(channel)) {
        		Optional<Long> realOwner = database.getOwner(sc.getId());
        		if(realOwner.isPresent()) {
        			if(realOwner.get() != owner.getId()) {
        				responses.add(String.format("%s: You are not the owner of `%s`. No changes made", owner.getNicknameMentionTag(), channel));
        			} else {
        				database.unregisterChannel(sc.getId());
        				responses.add(String.format("`%s` deleted", channel));
        				sc.delete();
        			}
        		} else {
        			responses.add(String.format("Unable to determine the ownerd of `%s`. No changes made", channel));
        		}
        	}
        }

        if(responses.isEmpty()) {
        	return String.format("No channels with the name `%s` was found", channel);
        } else {
        	return String.join("\n", responses);
        }
	}

	private Optional<ChannelCategory> getCategory(){
    	return api.getChannelCategoriesByNameIgnoreCase(category)
        		.stream().findFirst();
    }

	private String createChannel(List<String> args, User creator, Server server) {
		if(args.isEmpty()) {
            return "Missing channel name. Try `!channel create <chanel_name>`";
        }

        String channel = pop(args);

        if(!VALID_NAME.matcher(channel).matches()) {
        	return "Channel name can only contain alpha-numeric, underscores, and dashes";
        }

        ChannelCategory category = getCategory().get();

        boolean preExisting = category.getChannels().stream().anyMatch(sc -> sc.getName().equalsIgnoreCase(channel));
        if(preExisting) {
        	return String.format("Channels for `%s` already exists. No changes made.", channel);
        }

        getCategory().ifPresent(c -> {
        	Role everyone = server.getEveryoneRole();
        	Permissions defaultPermissions = c.getOverwrittenPermissions(everyone);
            server.createTextChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(everyone, defaultPermissions)
            	.addPermissionOverwrite(creator, playerPerms)
                .create()
                .thenAccept(tc -> database.registerChannel(tc.getId(), creator.getId(), LocalDateTime.now().plusDays(1)));

            server.createVoiceChannelBuilder()
            	.setName(channel)
            	.setCategory(c)
            	.addPermissionOverwrite(everyone, defaultPermissions)
            	.addPermissionOverwrite(creator, playerPerms)
            	.create()
            	.thenAccept(tc -> database.registerChannel(tc.getId(), creator.getId(), LocalDateTime.now().plusDays(1)));
        });

        return String.format(MSG_CHANNEL_CREATED, channel);
	}

}
