package com.bbaker.discord.redmarket;

import java.util.Collection;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.KnownCustomEmoji;

public class Table {


    int red, black;
    long mod;

    public Table() {
        this(0);
    }

    public Table(long mod) {
        this.mod = mod;
        red = (int) (Math.random() * 10) + 1;
        black = (int) (Math.random() * 10) + 1;
    }

    public int getBlack() {
        return black;
    }

    public int getRed() {
        return red;
    }

    public long getNet() {
        return black - red + mod;
    }

    public boolean isCrit() {
        return red == black;
    }

    public boolean isSuccess() {
        if(isCrit()) {
            return black % 2 == 0;
        } else {
            return getNet() > 0;
        }
    }

    public String getResults(DiscordApi api) {
        return getDiceFace("black", black, api) + " " + getDiceFace("red", red, api);
    }

    private String getDiceFace(String color, int face, DiscordApi api) {
        String name = String.format("%s_%02d", color, face);
        Collection<KnownCustomEmoji> emojies = api.getCustomEmojisByName(name);

        if(emojies.size() > 0) {
            return emojies.iterator().next().getMentionTag();
        } else {
            return color + ": " + face;
        }
    }

    public void setMod(long mod) {
        this.mod = mod;
    }

    public void setRed(int val) throws BadFormatException {
        if(val > 10) {
            throw new BadFormatException("`%d` must be between 1 and 10", val);
        }
        this.red = val;
    }

    public void setBlack(int val) throws BadFormatException {
        if(val > 10) {
            throw new BadFormatException("`%d` must be between 1 and 10", val);
        }
        this.black = val;
    }

}
