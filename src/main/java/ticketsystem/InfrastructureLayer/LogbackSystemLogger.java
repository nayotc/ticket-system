package ticketsystem.InfrastructureLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.ISystemLogger;

@Component
public class LogbackSystemLogger implements ISystemLogger {

    private static final Logger logger = LoggerFactory.getLogger(LogbackSystemLogger.class);

    @Override
    public void logEvent(String message, LogLevel level) {
        if (level == LogLevel.WARN) {
            logger.warn(message);
            return;
        }

        if (level == LogLevel.DEBUG) {
            logger.debug(message);
            return;
        }

        logger.info(message);
    }

    @Override
    public void logError(String errorMessage, Throwable exception) {
        logger.error(errorMessage, exception);
    }
}
