package edu.kit.kastel.formal.mimaflux.capacitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class Logger {
    public enum Level {
        DEBUG, INFO, ERROR, DEBUG_ERROR
    }
    public abstract void log(String message, Level level);

    public void info(String message) {
        log(message, Level.INFO);
    }

    public void debug(String message) {
        log(message, Level.DEBUG);
    }

    public void error(String message) {
        log(message, Level.ERROR);
    }

    public void logStacktrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        log(sw.toString(), Level.DEBUG_ERROR);
    }
}
