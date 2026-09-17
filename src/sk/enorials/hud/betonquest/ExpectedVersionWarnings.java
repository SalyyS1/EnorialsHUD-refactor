/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.betonquest;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class ExpectedVersionWarnings
implements Filter,
AutoCloseable {
    private final Logger logger;
    private final Filter previous;
    private final String serverVersion;
    private volatile boolean active = true;

    private ExpectedVersionWarnings(Logger logger, String string) {
        this.logger = logger;
        this.previous = logger.getFilter();
        this.serverVersion = string;
        logger.setFilter(this);
    }

    public static ExpectedVersionWarnings install(Logger logger, String string) {
        Filter filter = logger.getFilter();
        if (filter instanceof ExpectedVersionWarnings) {
            ExpectedVersionWarnings expectedVersionWarnings = (ExpectedVersionWarnings)filter;
            expectedVersionWarnings.close();
        }
        return new ExpectedVersionWarnings(logger, string);
    }

    @Override
    public boolean isLoggable(LogRecord logRecord) {
        if (this.previous != null && !this.previous.isLoggable(logRecord)) {
            return false;
        }
        if (!this.active || logRecord.getThrown() != null || !Level.WARNING.equals(logRecord.getLevel())) {
            return true;
        }
        if (!"BetonQuest".equals(logRecord.getLoggerName())) {
            return true;
        }
        for (String string : new String[]{"1.20.6", "1.21.4", "1.21.8"}) {
            String string2 = "Integration provided by 'BetonQuest' is not compatible due to policy: Minecraft version '" + string + "' and above is required.";
            if (!string2.equals(logRecord.getMessage()) || !ExpectedVersionWarnings.below(this.serverVersion, string)) continue;
            return false;
        }
        return true;
    }

    static boolean below(String string, String string2) {
        if (string == null || !string.matches("[0-9]+\\.[0-9]+(?:\\.[0-9]+)?")) {
            return false;
        }
        try {
            String[] stringArray = string.split("\\.");
            String[] stringArray2 = string2.split("\\.");
            for (int i = 0; i < 3; ++i) {
                int n;
                int n2 = i < stringArray.length ? Integer.parseInt(stringArray[i]) : 0;
                int n3 = n = i < stringArray2.length ? Integer.parseInt(stringArray2[i]) : 0;
                if (n2 == n) continue;
                return n2 < n;
            }
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        return false;
    }

    @Override
    public void close() {
        this.active = false;
        if (this.logger.getFilter() == this) {
            this.logger.setFilter(this.previous);
        }
    }
}

