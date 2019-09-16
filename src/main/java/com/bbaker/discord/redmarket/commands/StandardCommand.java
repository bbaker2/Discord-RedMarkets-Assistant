package com.bbaker.discord.redmarket.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.javacord.api.entity.message.Message;

import com.bbaker.discord.redmarket.exceptions.BadFormatException;

public interface StandardCommand {

	public void startup();
	public void shutdown();

	default int getInt(String val) throws BadFormatException {
        try {
            return Integer.valueOf(val);
        }catch (NumberFormatException e) {
            throw new BadFormatException("`%s` is not an integer.", val);
        }
    }

	default List<String> getArgs(Message message){
        String args = message.getContent();
        String[] argsArray = args.split("\\s+");
        List<String> argsList = new ArrayList<String>(Arrays.asList(argsArray));
        argsList.remove(0);
        return argsList;
    }

    default <T> T pop(List<T> stack) {
        return stack.remove(0);
    }

}
