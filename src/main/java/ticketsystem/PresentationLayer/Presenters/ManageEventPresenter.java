package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.PresentationLayer.Views.Management.CreateEvent;
import ticketsystem.PresentationLayer.Views.Management.HallMapBuilder;

@Component
public class ManageEventPresenter implements CreateEvent.CreateEventPresenter, HallMapBuilder.HallMapBuilderPresenter {

    private final EventService eventService;
    private final LotteryService lotteryService;
    private final LogbackSystemLogger logger;

    public ManageEventPresenter(EventService eventService, LotteryService lotteryService, LogbackSystemLogger logger) {
        this.eventService = eventService;
        this.lotteryService = lotteryService;
        this.logger = logger;
    }

    @Override
    public Long createEvent(CreateEvent.CreateEventRequest request) {
        Long eventId = null;

        try {
            validateCreateEventRequest(request);

            eventId = eventService.insertEvent(
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
                try {
                    lotteryService.addLottery(
                            request.sessionId(),
                            eventId,
                            request.lotteryWinnersNumber()
                    );
                } catch (Exception lotteryException) {
                    rollbackCreatedEvent(request.sessionId(), eventId);
                    logger.logEvent("Failed to create lottery for event " + eventId + ": " + lotteryException.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
                    throw new PresentationException("אירעה שגיאה בעת יצירת הגרלה. נסו שוב.");
                }
            }

            return eventId;

        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while creating event: " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת יצירת האירוע. נסו שוב.");
        }
    }

    @Override
    public EventDTO getEvent(String sessionId, Long eventId) {
        try {
            validateEventId(eventId);
            return eventService.getEvent(sessionId, eventId);
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while loading event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת טעינת פרטי האירוע. נסו שוב.");
        }
    }

    @Override
    public boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO) {
        try {
            validateDefineEventMapRequest(eventId, mapDTO);
            return Boolean.TRUE.equals(eventService.defineEventMap(sessionId, eventId, mapDTO));
        } catch (PresentationException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw presentationException(exception.getMessage());
        } catch (Exception exception) {
            logger.logEvent("Unexpected error while defining map for event " + eventId + ": " + exception.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("אירעה שגיאה בעת שמירת מפת האולם. נסו שוב.");
        }
    }

    private void rollbackCreatedEvent(String sessionId, Long eventId) {
        try {
            eventService.rollbackCreatedEvent(sessionId, eventId);
        } catch (Exception rollbackException) {
            logger.logEvent(rollbackException.getMessage(), LogbackSystemLogger.LogLevel.DEBUG);
            throw new PresentationException("יצירת ההגרלה נכשלה, האירוע לא נוצר. יש לפנות למנהל מערכת.");
        }
    }

    private void validateCreateEventRequest(CreateEvent.CreateEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("פרטי האירוע חסרים.");
        }

        if (request.hasLottery() && (request.lotteryWinnersNumber() == null || request.lotteryWinnersNumber() <= 0)) {
            throw new IllegalArgumentException("כאשר נבחרת הגרלה חובה להזין מספר זוכים חיובי.");
        }
    }

    private void validateDefineEventMapRequest(Long eventId, EventMapDTO mapDTO) {
        validateEventId(eventId);

        if (mapDTO == null) {
            throw new IllegalArgumentException("Map data cannot be null");
        }

        if (mapDTO.size() == null || mapDTO.size().first() == null || mapDTO.size().second() == null
                || mapDTO.size().first() <= 0 || mapDTO.size().second() <= 0) {
            throw new IllegalArgumentException("Map size must be positive");
        }

        if (mapDTO.elements() == null || mapDTO.elements().isEmpty()) {
            throw new IllegalArgumentException("Map must contain at least one element");
        }
    }

    private void validateEventId(Long eventId) {
        if (eventId == null || eventId <= 0) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
    }

    private String translateError(String message) {
        if (message == null || message.isBlank()) {
            return "אירעה שגיאה. נסו שוב.";
        }

        return switch (message) {
            case "Invalid token.", "Invalid session ID" -> "אנא התחבר מחדש";
            case "User does not have permission to create event for this company.",
                 "User does not have permission to create an event" -> "אין לך הרשאות ליצור אירוע עבור החברה הזו.";
            case "User does not have permission to define event map" -> "אין לך הרשאה להגדיר מפת אולם לאירוע הזה.";
            case "Event name cannot be empty." -> "שם האירוע לא יכול להיות ריק.";
            case "Event date cannot be in the past." -> "תאריך האירוע לא יכול להיות בעבר.";
            case "Event location cannot be empty." -> "יש לבחור מיקום לאירוע.";
            case "Traffic threshold must be a positive integer." -> "סף התנועה חייב להיות מספר שלם חיובי.";
            case "Event category cannot be empty." -> "קטגוריית האירוע לא יכולה להיות ריקה.";
            case "Artist name cannot be empty." -> "שם האמן לא יכול להיות ריק.";
            case "Price must be a positive number." -> "המחיר חייב להיות מספר חיובי.";
            case "Event not found" -> "האירוע לא נמצא.";
            case "Event ID cannot be null" -> "מזהה האירוע חסר.";
            case "Map data cannot be null" -> "פרטי המפה חסרים.";
            case "Map size must be positive" -> "גודל המפה חייב להיות חיובי.";
            case "Map must contain at least one element" -> "המפה חייבת להכיל לפחות אלמנט אחד.";
            case "Event map must contain at least one seating area or standing area" -> "מפת האירוע חייבת להכיל לפחות אזור ישיבה או אזור עמידה אחד.";
            default -> message;
        };
    }

    private PresentationException presentationException(String message) {
        return new PresentationException(translateError(message));
    }
}
