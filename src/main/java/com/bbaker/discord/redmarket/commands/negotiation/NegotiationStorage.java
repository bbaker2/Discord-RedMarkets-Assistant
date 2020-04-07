package com.bbaker.discord.redmarket.commands.negotiation;

import java.util.Optional;

import com.bbaker.discord.redmarket.Tracker;

public interface NegotiationStorage {

    public Optional<Tracker> getTracker(long channelId);

    public void storeTracker(long channelId, Tracker tracker);


}
