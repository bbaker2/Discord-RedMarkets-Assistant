package com.bbaker.discord.redmarket.commands.negotiation;

public enum Phase {
    NEGOTIATION,    // Can add sway, and trigger the next round
    CLOSING,		// No more rounds. Must make a closing leadership check
    UNDERCUT,		// Must make a CHA check to not be undercut
    FINISHED			// After the closing leadership check and after the undercut phase
}
