package ticketsystem.ApplicationLayer;

public interface ISystemLogger {

    void logEvent(String useCase, String details);

    void logError(String useCase, String errorMessage, Throwable exception);
}
