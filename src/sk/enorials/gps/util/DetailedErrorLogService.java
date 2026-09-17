/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.gps.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import sk.enorials.gps.EnorialsGpsPlugin;

public final class DetailedErrorLogService {
    private final EnorialsGpsPlugin plugin;
    private Handler handler;

    public DetailedErrorLogService(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
    }

    public void install() {
        if (this.handler != null) {
            return;
        }
        try {
            File file = new File(this.plugin.getDataFolder(), "logs");
            if (!file.exists()) {
                file.mkdirs();
            }
            File file2 = new File(file, "errors.log");
            this.handler = new DailyAppendFileHandler(file2);
            this.handler.setLevel(Level.ALL);
            this.handler.setFilter(new PluginErrorFilter());
            this.handler.setFormatter(new ErrorFormatter());
            Logger.getLogger("").addHandler(this.handler);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to initialize detailed error log file: " + iOException.getMessage());
        }
    }

    public void uninstall() {
        if (this.handler == null) {
            return;
        }
        Logger.getLogger("").removeHandler(this.handler);
        this.handler.close();
        this.handler = null;
    }

    private static final class DailyAppendFileHandler
    extends Handler {
        private final File file;

        private DailyAppendFileHandler(File file) throws IOException {
            this.file = file;
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        @Override
        public synchronized void publish(LogRecord logRecord) {
            if (!this.isLoggable(logRecord)) {
                return;
            }
            try {
                String string = this.getFormatter().format(logRecord);
                Files.writeString(this.file.toPath(), (CharSequence)string, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static final class PluginErrorFilter
    implements Filter {
        private PluginErrorFilter() {
        }

        @Override
        public boolean isLoggable(LogRecord logRecord) {
            if (logRecord == null || logRecord.getLevel().intValue() < Level.WARNING.intValue()) {
                return false;
            }
            String string = PluginErrorFilter.safe(logRecord.getLoggerName()).toLowerCase(Locale.ROOT);
            String string2 = PluginErrorFilter.safe(logRecord.getMessage()).toLowerCase(Locale.ROOT);
            if (string.contains("enorialsgps") || string2.contains("enorialsgps")) {
                return true;
            }
            for (Throwable throwable = logRecord.getThrown(); throwable != null; throwable = throwable.getCause()) {
                for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                    if (!stackTraceElement.getClassName().startsWith("sk.enorials.gps")) continue;
                    return true;
                }
            }
            return false;
        }

        private static String safe(String string) {
            return string == null ? "" : string;
        }
    }

    private static final class ErrorFormatter
    extends Formatter {
        private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        private ErrorFormatter() {
        }

        @Override
        public String format(LogRecord logRecord) {
            StringBuilder stringBuilder = new StringBuilder(512);
            stringBuilder.append("[").append(this.fmt.format(new Date(logRecord.getMillis()))).append("] ");
            stringBuilder.append("[").append(logRecord.getLevel().getName()).append("] ");
            stringBuilder.append("[logger=").append(logRecord.getLoggerName()).append("] ");
            stringBuilder.append("[thread=").append(logRecord.getThreadID()).append("] ");
            stringBuilder.append(this.formatMessage(logRecord)).append(System.lineSeparator());
            if (logRecord.getThrown() != null) {
                stringBuilder.append(this.stackTrace(logRecord.getThrown())).append(System.lineSeparator());
            }
            return stringBuilder.toString();
        }

        private String stackTrace(Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            return stringWriter.toString();
        }
    }
}

