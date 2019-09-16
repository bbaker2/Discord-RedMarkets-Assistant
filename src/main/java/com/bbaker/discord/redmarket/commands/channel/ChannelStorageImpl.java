package com.bbaker.discord.redmarket.commands.channel;

import java.time.LocalDateTime;
import java.util.Optional;

import com.bbaker.discord.redmarket.db.DatabaseService;

public class ChannelStorageImpl implements ChannelStorage {

	public static final String TABLE_CHANNEL = "CHANNEL";

	private DatabaseService db = null;


	public ChannelStorageImpl(DatabaseService dbService) {
		this.db = dbService;
	}

	@Override
	public void createTables() {
		if(!db.hasTable(TABLE_CHANNEL)) {
            System.out.println("Creating " + TABLE_CHANNEL);
            String tableInsert = db.query(
                    "CREATE TABLE %s ("+
                        "id 		BIGINT 			SERIAL PRIMARY KEY, "+
                        "user_id 	BIGINT 			NOT NULL, "+
                        "channel_id	BIGINT 			NOT NULL UNIQUE, "+
                        "expiration TIMESTAMP 		NOT NULL "+
                    ");"
                    ,TABLE_CHANNEL);

            db.useHandle(handler -> {
                handler.execute(tableInsert);
            });
        } else {
        	System.out.println(TABLE_CHANNEL + " already created.");
        }
	}

	@Override
	public void registerChannel(long channelId, long userId, LocalDateTime expiration) {
		String q = db.query("INSERT INTO %s(CHANNEL_ID, USER_ID, EXPIRATION) VALUES(:channel, :user, :expiration)", TABLE_CHANNEL);
		db.withHandle(h ->
			h.createUpdate(q)
				.bind("user", userId)
				.bind("channel", channelId)
				.bind("expiration", expiration)
				.execute()
		);
	}

	@Override
	public Optional<Long> getOwner(long channelId) {
		String q = db.query("SELECT USER_ID FROM %s WHERE channel_id = :channel", TABLE_CHANNEL);
		return db.withHandle(h ->
			h.select(q)
				.bind("channel", channelId)
				.mapTo(Long.class)
				.findOne()
		);

	}

	@Override
	public void unregisterChannel(long channelId) {
		String q = db.query("delete from %s where CHANNEL_ID = :channelId", TABLE_CHANNEL);
        db.useHandle(h -> h.createUpdate(q).bind("channelId", channelId).execute());
	}

}
