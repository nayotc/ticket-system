package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.PresentationLayer.Views.Management.CreateEvent;

@Component
public class EventPresenter implements CreateEvent.CreateEventPresenter {

    private final EventService eventService;
    private final LotteryService lotteryService;

    public EventPresenter(EventService eventService, LotteryService lotteryService) {
        this.eventService = eventService;
        this.lotteryService = lotteryService;
    }

    @Override
    public Long createEvent(CreateEvent.CreateEventRequest request) {
        try {
            validateRequest(request);

            Long eventId = eventService.insertEvent(
                    request.sessionId(),
                    request.eventName(),
                    request.companyId(),
                    request.date(),
                    request.location(),
                    request.trafficThreshold(),
                    request.category(),
                    request.artist(),
                    request.price(),
                    request.mapHeight(),
                    request.mapWidth()
            );

            if (request.hasLottery()) {
                lotteryService.addLottery(
                        request.sessionId(),
                        eventId,
                        request.lotteryWinnersNumber()
                );
            }

            return eventId;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            throw new PresentationException("אירעה שגיאה בעת יצירת האירוע. נסו שוב.");
        }
    }

    private void validateRequest(CreateEvent.CreateEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("פרטי האירוע חסרים.");
        }

        if (request.hasLottery() && (request.lotteryWinnersNumber() == null || request.lotteryWinnersNumber() <= 0)) {
            throw new IllegalArgumentException("כאשר נבחרת הגרלה חובה להזין מספר זוכים חיובי.");
        }
    }

    private String successMessage(CreateEvent.CreateEventRequest request) {
        if (request.hasLottery()) {
            return "האירוע נוצר בהצלחה ונפתחה עבורו הגרלת זכות רכישה.";
        }

        return "האירוע נוצר בהצלחה.";
    }

    private String TranslateError(String message) {
        if (message == null || message.isBlank()) {
            return "אירעה שגיאה בעת יצירת האירוע. נסו שוב.";
        }

        return switch (message) {
            case "Invalid token." -> "אנא התחבר מחדש";
            case "User does not have permission to create event for this company." -> "אין לך הרשאות ליצור אירוע עבור החברה הזו.";
            case "Event name cannot be empty." -> "שם האירוע לא יכול להיות ריק.";
            case "Event date cannot be in the past." -> "תאריך האירוע לא יכול להיות בעבר.";
            case "Event location cannot be empty." -> "יש לבחור מיקום לאירוע";
            case "Traffic threshold must be a positive integer." -> "סף התנועה חייב להיות מספר שלם חיובי.";
            case "Event category cannot be empty." -> "קטגוריית האירוע לא יכולה להיות ריקה.";
            case "Artist name cannot be empty." -> "שם האמן לא יכול להיות ריק.";
            case "Price must be a positive number." -> "המחיר חייב להיות מספר חיובי.";
            default -> message;
        };

    }

    private PresentationException presentationException(String message) {
        return new PresentationException(TranslateError(message));
    }
}
