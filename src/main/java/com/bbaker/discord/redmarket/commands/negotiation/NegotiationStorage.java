package com.bbaker.discord.redmarket.commands.negotiation;

import java.util.Optional;

public interface NegotiationStorage {

    public void createTable();

    public Optional<Tracker> getTracker(long channelId);

    public void storeTracker(long channelId, Tracker tracker);


}
