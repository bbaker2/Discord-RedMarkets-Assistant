package com.bbaker.discord.redmarket;

import java.util.OptionalInt;

import com.bbaker.arg.parser.exception.BadArgumentException;
import com.bbaker.arg.parser.text.TextArgumentProcessor;
import static com.bbaker.arg.parser.text.TextArgumentEvaluator.getTotal;

public class SwayProcessor implements TextArgumentProcessor {

    private Tracker tracker;

    public SwayProcessor(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public boolean evaluate(String token, OptionalInt left, OptionalInt right) throws BadArgumentException {
        Sway s = findSway(token);
        int total = getTotal(left, right);
        switch(s) {
            case Client:
                tracker.swayClient(total);
                break;
            case Provider:
                tracker.swayProvider(total);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean isToken(String token) {
        return findSway(token) != null;
    }

    private static Sway findSway(String token) {
        switch(token.toLowerCase()) {
            case "p":
            case "provider":
                return Sway.Provider;
            case "c":
            case "client":
                return Sway.Client;
            default:
                return null;
        }
    }

}
