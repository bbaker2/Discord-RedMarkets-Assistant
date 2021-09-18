package com.bbaker.discord.redmarket.commands.roll;

import com.bbaker.slashcord.structure.entity.IntOption;

public class ModOption extends IntOption {

    public ModOption() {
        super("mod", "Adjust the NET result of the red/black die", false);
    }

}
