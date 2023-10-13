package edu.kit.kastel.formal.mimaflux.capacitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class Logger {
    public enum Level {
        DEBUG, INFO, ERROR, DEBUG_ERROR
    }

    public abstract void log(LogProducer message, Level level);

    public void info(LogProducer logProducer) {
        log(logProducer, Level.INFO);
    }

    public void info(String message) {
        info(() -> message);
    }

    public void debug(LogProducer logProducer) {
        log(logProducer, Level.DEBUG);
    }

    public void debug(String message) {
        debug(() -> message);
    }

    public void error(LogProducer logProducer) {
        log(logProducer, Level.ERROR);
    }

    public void error(String message) {
        error(() -> message);
    }

    public void logStacktrace(Exception exception) {
        log(() -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            return sw.toString();
        }, Level.DEBUG_ERROR);
    }

    @FunctionalInterface
    public interface LogProducer {
        String generateLog();
    }
}
