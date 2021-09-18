package com.bbaker.discord.redmarket.commands.roll;

import com.bbaker.slashcord.structure.entity.IntChoice;
import com.bbaker.slashcord.structure.entity.IntOption;

public class RedOption extends IntOption {

    public RedOption() {
        super("red", "Force set the RED die face", false);
        appendChoice(
            new IntChoice("1", 1),
            new IntChoice("2", 2),
            new IntChoice("3", 3),
            new IntChoice("4", 4),
            new IntChoice("5", 5),
            new IntChoice("6", 6),
            new IntChoice("7", 7),
            new IntChoice("8", 8),
            new IntChoice("9", 9),
            new IntChoice("10", 10)
        );
    }

}
