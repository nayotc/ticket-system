package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.PresentationLayer.Views.WaitingQueue;

@Component
public class WaitingQueuePresenter implements WaitingQueue.WaitingQueuePresenter {

    private final WaitingQueueService waitingQueueService;

    public WaitingQueuePresenter(WaitingQueueService waitingQueueService) {
        this.waitingQueueService = waitingQueueService;
    }

    @Override
    public WaitingQueueSnapshot getQueueSnapshot(long eventId, String sessionToken) {
        try {
            validateSessionToken(sessionToken);

            String eventName = waitingQueueService.getQueueEventName(eventId);
            int position = waitingQueueService.getQueuePosition(eventId, sessionToken);

            if (position > 0) {
                int estimatedWaitMinutes = waitingQueueService.estimateWaitMinutes(eventId, sessionToken);
                return WaitingQueueSnapshot.waiting(eventName, position, estimatedWaitMinutes);
            }

            return WaitingQueueSnapshot.ready(eventName);

        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Sold Out")) {
                return WaitingQueueSnapshot.soldOut("האירוע");
            }

            return WaitingQueueSnapshot.error("האירוע", "לא ניתן לטעון את מצב התור כרגע");

        } catch (IllegalArgumentException e) {
            return WaitingQueueSnapshot.error("האירוע", translateQueueError(e.getMessage()));

        } catch (Exception e) {
            return WaitingQueueSnapshot.error("האירוע", "לא ניתן לטעון את מצב התור כרגע");
        }
    }

    @Override
    public void leaveQueue(long eventId, String sessionToken) {
        try {
            validateSessionToken(sessionToken);
            waitingQueueService.leaveQueue(eventId, sessionToken);

        } catch (IllegalArgumentException e) {
            throw new PresentationException(translateQueueError(e.getMessage()));

        } catch (Exception e) {
            throw new PresentationException("לא ניתן לצאת מהתור כרגע.");
        }
    }

    private void validateSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private String translateQueueError(String message) {
        if (message == null || message.isBlank()) {
            return "לא ניתן לטעון את מצב התור כרגע.";
        }

        if (message.contains("Invalid token")
                || message.contains("token")
                || message.contains("Token")
                || message.contains("session")
                || message.contains("Session")) {
            return "החיבור למערכת לא תקין. התחברי מחדש ונסי שוב.";
        }

        if (message.contains("Event not found")) {
            return "האירוע לא נמצא.";
        }

        return "לא ניתן לטעון את מצב התור כרגע.";
    }
}