package com.bbaker.discord.redmarket.roller;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bbaker.discord.redmarket.exceptions.BadFormatException;

public class DiceRoller {

    private static final Pattern diceRgx = Pattern.compile("(\\d+)?(red|r|black|b)(\\d+)?");
    private static final Pattern modRgx = Pattern.compile("((\\+?|-?)\\d+)");

    private Table table = new Table();

    public boolean processToken(String token) throws BadFormatException {
        return diceParser(token) || modeParser(token);
    }

    private boolean modeParser(String token) throws BadFormatException {
        Matcher m = modRgx.matcher(token);
        if(m.find()) {
            return false;
        }

        try {
            long mod = Long.valueOf(m.group(1));
            table.setMod(mod);
        } catch(NumberFormatException nfe) {
            throw new BadFormatException(String.format("'%s' is not a integer", m.group(1)));
        }
        return true;
    }

    private boolean diceParser(String token) throws BadFormatException {
        Matcher m = diceRgx.matcher(token);
        if(!m.find()) {
            return false;
        }

        OptionalInt left = getCount(m.group(1));
        String die = m.group(2);
        OptionalInt right = getCount(m.group(3));

        if(left.isPresent() && right.isPresent()) {
            throw new BadFormatException("You cannot have a number on the left AND right side. Pick %d or %d. Not both.",
                    left.getAsInt(), right.getAsInt());
        }

        int side = left.orElseGet(() -> right.getAsInt());

        switch(die) {
            case "red":
            case "r":
                table.setRed(side);
                break;
            case "black":
            case "b":
                table.setBlack(side);
                break;
        }

        return true;
    }

    public Table getTable() {
        return table;
    }

    /**
     * Never throws an exception. Assumes 0 if anything goes wrong
     * @param val
     * @return the numeric value of a string. 0 if unsuccessful for any reason
     */
    private OptionalInt getCount(String val) {
        if(val == null) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.valueOf(val));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

}
