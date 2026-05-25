package ticketsystem.PresentationLayer.Presenters;

/**
 * Exception used by Presenters to expose user-facing error messages to the View.
 *
 * Application services may throw technical or domain-related exceptions, or return
 * failure values. Presenters translate those failures into PresentationException
 * so the View can display a clean message to the user.
 */
public class PresentationException extends RuntimeException {

    public PresentationException(String message) {
        super(message);
    }
}
