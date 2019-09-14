package com.bbaker.discord.redmarket.commands;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class ChannelCommand implements CommandExecutor {

    private DiscordApi api = null;
    private String category = null;

    public ChannelCommand(DiscordApi api, String category) {
        this.api = api;
        this.category = category.toLowerCase();
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

        server.getChannelCategoriesByNameIgnoreCase(category)
                .stream().findFirst().ifPresent(c -> {
            server.createChannelCategoryBuilder()
                .setName(channel);
        });


        return "";
    }

}
