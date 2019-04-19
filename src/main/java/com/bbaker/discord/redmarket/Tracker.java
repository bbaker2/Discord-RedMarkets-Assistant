package com.bbaker.discord.redmarket;

public class Tracker {
    private int provider, client, rounds, current;
    private boolean secret;

    private int swayClient, swayProvider;

    private String[] tracker = new String[] {
        "As A Favor", "Buyer's Market", "At Value", "Labor", "Hazard Pay", "100% Mark-Up", "Expenses"
    };

    public Tracker(Table t) {
        if(t.isSuccess()) {
            provider = t.isCrit() ? 2 : 1;
            secret = false;
            rounds = (int)Math.round(t.getBlack()/2 + 0.5);
        } else {
            provider = t.isCrit() ? 0 : 1;
            secret = true;
            rounds = (int) (Math.random() * 5) + 1; // simulates a 1d10 / 2
        }
        client = 6;
        current = 1;

        swayClient = 1;
        swayProvider = 0;
    }


    public String getProviderTrack() {
        return tracker[provider];
    }

    public int getProvider() {
        return provider;
    }

    public int getClient() {
        return client;
    }

    public String getClientTrack() {
        return tracker[client];
    }

    public int getTotalRounds() {
        return rounds;
    }

    public int getCurrentRound() {
        return current;
    }

    public boolean isSecret() {
        return secret;
    }

    public int getSwayClient() {
        return swayClient;
    }

    public void swayClient(int mod) {
        swayClient += mod;
    }

    public void swayProvider(int mod) {
        swayProvider += mod;
    }

    public int getSwayProvider() {
        return swayProvider;
    }

    public void next() {
        while(swayProvider-- > 0) {
            provider++;
            if(provider >= client) {
                client++;
            }
        }

        while(swayClient-- > 0) {
            client--;
            if(provider <= client) {
                provider--;
            }
        }

        current++;
        swayClient = 1; // client always start with one successful sway
        swayProvider = 0;

    }

}
