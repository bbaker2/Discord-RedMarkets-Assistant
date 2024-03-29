package com.bbaker.discord.redmarket.commands.negotiation;

import static java.lang.Math.*;

import com.bbaker.discord.redmarket.commands.roll.Table;

public class Tracker {
    private int provider, client, rounds, current;
    private boolean secret;
    private Phase phase;

    private int swayClient, swayProvider;

    private String[] tracker = new String[] {
        "As A Favor", "Buyer's Market", "At Value", "Labor", "Hazard Pay", "100% Mark-Up", "Expenses"
    };

    public Tracker(int provider, int client, int rounds, int current, int swayClient, int swayProvider, boolean secret, Phase phase) {
        this.provider = provider;
        this.client = client;
        this.rounds = rounds;
        this.current = current;
        this.swayClient = swayClient;
        this.swayProvider = swayProvider;
        this.secret = secret;
        this.phase = phase;
    }

    public Tracker(Table t) {
        if(t.isSuccess()) {
            provider = t.isCrit() ? 2 : 1;
            secret = false;
            long leadership = t.getBlack()+t.getMod();
            rounds = (int)Math.ceil(leadership/2.0);
        } else {
            provider = t.isCrit() ? 0 : 1;
            secret = true;
            rounds = (int) (Math.random() * 5) + 1; // simulates a 1d10 / 2
        }

        rounds = min(5, rounds); // there cannot be more than 5 rounds
        rounds = max(1, rounds); // there cannot be less than 1 round
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

        int diff = client - provider;
        // If the client and provider will not collide, just slide them
        if(diff > swayProvider + swayClient) {
            provider += swayProvider;
            client -= swayClient;
        } else {
            diff--; // for the sake of these calculations, the diff is reduced by one to represent exclusive diff
            // Figure out at which point the provider and client will meet
            provider += (int) (diff/2.0); 		// round down
            client -= 	(int) (diff/2.0+.5); 	//round up

            // Now calculate which direction the left over sway will push both
            int finalSway = swayProvider - swayClient;
            provider += finalSway;
            client += finalSway;
        }

        provider = min(provider, 5); // the provider cannot be greater than position 5
        client = min(client, 6); // the client cannot be be greater than position 6


        provider = max(0, provider); // the provider cannot be less than position 0
        client = max(client, 1); // the client cannot be less than position 1

        current = min(rounds, current+1); // do not let the current round exceed the total
        swayClient = 1;
        swayProvider = 0;

    }

    public void close(int finalPrice) {
        provider = finalPrice;
        client = finalPrice;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }


}
