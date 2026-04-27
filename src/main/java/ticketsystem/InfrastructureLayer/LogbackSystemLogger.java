package ticketsystem.InfrastructureLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ticketsystem.ApplicationLayer.ISystemLogger;

public class LogbackSystemLogger implements ISystemLogger {

    private static final Logger eventLogger = LoggerFactory.getLogger("EventLogger");
    private static final Logger errorLogger = LoggerFactory.getLogger("ErrorLogger");

    @Override
    public void logEvent(String useCase, String details) {
        eventLogger.info("[{}] - {}", useCase, details);
    }

    @Override
    public void logError(String useCase, String errorMessage, Throwable exception) {
        errorLogger.error("[{}] - System Error: {}", useCase, errorMessage, exception);
    }
}
