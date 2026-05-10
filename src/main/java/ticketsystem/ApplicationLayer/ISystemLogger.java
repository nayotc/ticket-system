package ticketsystem.ApplicationLayer;

public interface ISystemLogger {

    enum LogLevel {
        INFO,
        WARN,
        DEBUG
    }

    void logEvent(String message, LogLevel level);

    void logError(String errorMessage, Throwable exception);
}
