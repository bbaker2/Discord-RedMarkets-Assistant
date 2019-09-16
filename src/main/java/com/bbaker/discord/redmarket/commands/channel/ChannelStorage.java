package com.bbaker.discord.redmarket.commands.channel;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChannelStorage {

	/**
	 * Create the tables
	 */
	public void createTables();

	/**
	 * @param channel the channel being registered
	 * @param user the user who is creating the channel
	 * @param localDateTime when the channel should self-delete
	 */
	public void registerChannel(long channel, long user, LocalDateTime localDateTime);


	/**
	 * @param channel the channel you are checking against
	 * @return The Id of the user who registered the channel, if the channel exists
	 */
	public Optional<Long> getOwner(long channel);


	/**
	 * @param channel the channel that will be deleted from our records
	 */
	public void unregisterChannel(long channel);

}
