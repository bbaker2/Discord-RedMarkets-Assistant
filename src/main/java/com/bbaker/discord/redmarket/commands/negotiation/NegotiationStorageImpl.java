package com.bbaker.discord.redmarket.commands.negotiation;

import java.util.Optional;

import com.bbaker.discord.redmarket.db.DatabaseService;

public class NegotiationStorageImpl implements NegotiationStorage {

    public static final String TABLE_CHANNEL = "NEGOTIATION";

    private DatabaseService db = null;


    public NegotiationStorageImpl(DatabaseService dbService) {
        this.db = dbService;
    }

    @Override
    public void createTable() {
        if(db.hasTable(TABLE_CHANNEL)) {
            return;
        }

        System.out.println("Creating " + TABLE_CHANNEL);
        String tableInsert = db.query(
                "CREATE TABLE %s ("+
                    "id             BIGINT          SERIAL PRIMARY KEY, "+
                    "channel_id     BIGINT          NOT NULL UNIQUE, "+
                    "round          INT             NOT NULL, "+
                    "total          INT             NOT NULL, "+
                    "client         INT             NOT NULL, "+
                    "provider       INT             NOT NULL, "+
                    "sway_provider  INT             NOT NULL, "+
                    "sway_client    INT             NOT NULL, "+
                    "is_secret      BOOLEAN         NOT NULL "+
                ");"
                ,TABLE_CHANNEL);

        db.useHandle(handler -> {
            handler.execute(tableInsert);
        });

    }

    @Override
    public Optional<Tracker> getTracker(long channelId) {
        String q = db.query("SELECT CHANNEL_ID, ROUND, TOTAL, CLIENT, PROVIDER, SWAY_PROVIDER, SWAY_CLIENT, IS_SECRET "+
                            "FROM %s "+
                            "WHERE CHANNEL_ID = :channelId", TABLE_CHANNEL);
        return db.withHandle(h ->
            h.select(q)
                .bind("channelId", channelId)
                .map(new TrackerMapper())
                .findFirst()
        );
    }

    @Override
    public void storeTracker(long channelId, Tracker tracker) {
        boolean exists = getTracker(channelId).isPresent();

        String query;
        if(exists) {
            query = db.query("UPDATE %s " +
                        "SET CHANNEL_ID = :channelId, ROUND = :round, TOTAL = :total, CLIENT = :client, PROVIDER = :provider, "+
                        "SWAY_PROVIDER = :swayProvider, SWAY_CLIENT = :swayClient, IS_SECRET = :isSecret WHERE CHANNEL_ID = :channelId", TABLE_CHANNEL);
        } else {
            query = db.query("INSERT INTO %s " +
                    "(CHANNEL_ID, ROUND, TOTAL, CLIENT, PROVIDER, SWAY_PROVIDER, SWAY_CLIENT, IS_SECRET) " +
                    "VALUES(:channelId, :round, :total, :client, :provider, :swayProvider, :swayClient, :isSecret)", TABLE_CHANNEL);
        }

        db.useHandle(h ->
            h.createUpdate(query)
            .bind("channelId",      channelId)
            .bind("round",          tracker.getCurrentRound())
            .bind("total",          tracker.getTotalRounds())
            .bind("client",         tracker.getClient())
            .bind("provider",       tracker.getProvider())
            .bind("swayProvider",   tracker.getSwayProvider())
            .bind("swayClient",     tracker.getSwayClient())
            .bind("isSecret",       tracker.isSecret())
            .execute()
        );

    }


}
