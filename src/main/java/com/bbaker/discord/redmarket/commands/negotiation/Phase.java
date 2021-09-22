package com.bbaker.discord.redmarket.commands.negotiation;

public enum Phase {
    NEGOTIATION(1), // Can add sway, and trigger the next round
    CLOSING(2),		// No more rounds. Must make a closing leadership check
    UNDERCUT(3),	// Must make a CHA check to not be undercut
    FINISHED(4);	// After the closing leadership check and after the undercut phase

    private int dbVal;

    Phase(int dbVal) {
        this.dbVal = dbVal;
    }

    public int getDbVal() {
        return this.dbVal;
    }

    public static Phase valueOf(int dbVal) {
        for(Phase each : values()) {
            if(each.getDbVal() == dbVal) {
                return each;
            }
        }
        return null;
    }
}
