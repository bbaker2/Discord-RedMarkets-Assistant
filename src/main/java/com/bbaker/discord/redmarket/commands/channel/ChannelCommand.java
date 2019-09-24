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
import com.bbaker.discord.redmarket.exceptions.ServerException;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class ChannelCommand implements CommandExecutor, StandardCommand {

    public static final String MSG_NO_OWNER = "%s: Unable to determine the ownerd of `%s`. No changes made";
	public static final String MSG_DUPLICATE_CHANNEL = "Channels for `%s` already exists. No changes made.";
    public static final String MSG_BAD_CHAN_NAME = "Channel name can only contain alpha-numeric, underscores, and dashes";
    public static final String MSG_CHANNEL_NOT_FOUND = "%s: No channels with the name `%s` was found. No changes made";
    public static final String MSG_NOT_OWNER = "%s: You are not the owner of `%s`. No changes made";
    public static final String MSG_CHANNEL_DELETED = "%s: Channels `%s` were deleted";
    public static final String MSG_CHANNEL_CREATED = "Channel `%s` created.";

    private DiscordApi api = null;
    private Permissions playerPerms = null;
    private String category = null;
    private Pattern VALID_NAME = Pattern.compile("[\\w|\\-]+");
    private ChannelStorage database;

    public ChannelCommand(DiscordApi api, String category, ChannelStorage storage) {
        this.api = api;
        this.category = category;
        this.database = storage;
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

        try {
            switch (action.toLowerCase()) {
            case "create":
            case "c":
                return createChannel(args, creator, server);
            case "delete":
            case "d":
                return deleteChannel(args, creator, message.getMentionedChannels(), server);
            case "add":
            case "a":
                return addUser(args, creator, server);
            default:
                return String.format("Unknown action `%s`. Supported actions: `create`", action);
            }
        } catch (ServerException e) {
            return e.getMessage();
        }
    }

    private String addUser(List<String> args, User creator, Server server) {
        // TODO Auto-generated method stub
        return null;
    }

    private String deleteChannel(List<String> args, User owner, List<ServerTextChannel> mentioned, Server server) throws ServerException {
        if(args.isEmpty()) {
            return "Missing channel name. Try `!channel remove <chanel_name>`";
        }

        String channel = pop(args);

        ChannelCategory category = getCategory();

        // If someone used a #mentioned tag, attempt to convert it into this string name instead
        for(ServerTextChannel stc : mentioned) {
            if(stc.getMentionTag().contentEquals(channel)) {
                channel = stc.getName();
                break;
            }
        }

        // Only search in the list of channels that are in the approved category
        List<ServerChannel> channelList = category.getChannels();

        // Loop over all the channels and see if:
        // 1.) The channels has an owner
        // 2.) If the owner is the one trying to delete it
        // Otherwise, record an error
        List<ServerChannel> toDelete = new ArrayList<ServerChannel>();
        for(ServerChannel sc : channelList) {
            if(sc.getName().equalsIgnoreCase(channel)) {
                Optional<Long> realOwner = database.getOwner(sc.getId());
                if(realOwner.isPresent()) {
                    if(realOwner.get() != owner.getId()) {
                        return String.format(MSG_NOT_OWNER, owner.getNicknameMentionTag(), channel);
                    } else {
                        toDelete.add(sc);
                    }
                } else {
                    return String.format(MSG_NO_OWNER, owner.getNicknameMentionTag(), channel);
                }
            }
        }

        // if no messages were generated, it is safe to assume we were unsuccessful at finding a match
        if(toDelete.isEmpty()) {
            return String.format(MSG_CHANNEL_NOT_FOUND, owner.getNicknameMentionTag(), channel);
        } else {
            for(ServerChannel sc : toDelete) {
                database.unregisterChannel(sc.getId()); // remove the owner from the database
                sc.delete(); // actually delete the channel
            }
            return String.format(MSG_CHANNEL_DELETED, owner.getNicknameMentionTag(), channel);
        }
    }

    private ChannelCategory getCategory() throws ServerException{
        return api.getChannelCategoriesByNameIgnoreCase(category)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ServerException("No category named `%s` found. Unable to continue.", category));
    }

    private String createChannel(List<String> args, User creator, Server server) throws ServerException {
        if(args.isEmpty()) {
            return "Missing channel name. Try `!channel create <chanel_name>`";
        }

        String channel = pop(args);

        if(!VALID_NAME.matcher(channel).matches()) {
            return MSG_BAD_CHAN_NAME;
        }

        ChannelCategory chanCategory = getCategory();

        boolean preExisting = chanCategory.getChannels().stream().anyMatch(sc -> sc.getName().equalsIgnoreCase(channel));
        if(preExisting) {
            return String.format(MSG_DUPLICATE_CHANNEL, channel);
        }

        Role everyone = server.getEveryoneRole();
        Permissions defaultPermissions = chanCategory.getOverwrittenPermissions(everyone);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        server.createTextChannelBuilder()
            .setName(channel)
            .setCategory(chanCategory)
            .addPermissionOverwrite(everyone, defaultPermissions)
            .addPermissionOverwrite(creator, playerPerms)
            .create()
            .thenAccept(tc -> database.registerChannel(tc.getId(), creator.getId(), tomorrow));

        server.createVoiceChannelBuilder()
            .setName(channel)
            .setCategory(chanCategory)
            .addPermissionOverwrite(everyone, defaultPermissions)
            .addPermissionOverwrite(creator, playerPerms)
            .create()
            .thenAccept(vc -> database.registerChannel(vc.getId(), creator.getId(), tomorrow));

        return String.format(MSG_CHANNEL_CREATED, channel);
    }

}
