package ticketsystem.InfrastructureLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ticketsystem.ApplicationLayer.ISystemLogger;

public class LogbackSystemLogger implements ISystemLogger {

    private static final Logger logger = LoggerFactory.getLogger(LogbackSystemLogger.class);

    @Override
    public void logEvent(String message, LogLevel level) {
        switch (level) {
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
        }
    }

    @Override
    public void logError(String errorMessage, Throwable exception) {
        logger.error(errorMessage, exception);
    }
}
