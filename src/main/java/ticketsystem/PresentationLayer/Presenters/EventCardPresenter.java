package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

@Component
public class EventCardPresenter {

    private final LotteryService lotteryService;
    private final WaitingQueueService waitingQueueService;

    public EventCardPresenter(
            LotteryService lotteryService,
            WaitingQueueService waitingQueueService
    ) {
        this.lotteryService = lotteryService;
        this.waitingQueueService = waitingQueueService;
    }

    public String purchaseRoute(Long eventId) {
        validateEventId(eventId);
        return UiRoutes.TICKET_SELECTION.replace(":eventId", String.valueOf(eventId));
    }

    public String lotteryRegistrationRoute(Long eventId) {
        validateEventId(eventId);
        return UiRoutes.LOTTERY_REGISTRATION.replace(":eventId", String.valueOf(eventId));
    }

    public boolean isPreSaleCodeValid(String memberToken, Long eventId, String lotteryCode) {
        try {
            if (memberToken == null || memberToken.isBlank()) {
                throw new PresentationException("יש להתחבר כדי להשתמש בקוד זכייה בהגרלה.");
            }

            validateEventId(eventId);

            if (lotteryCode == null || lotteryCode.isBlank()) {
                return false;
            }

            return lotteryService.validateWinnerCodeForEvent(memberToken, eventId, lotteryCode);

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateLotteryError(e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException("לא הצלחנו לבדוק את קוד ההגרלה. נסו שוב.");
        }
    }

    private void validateEventId(Long eventId) {
        if (eventId == null || eventId <= 0) {
            throw new PresentationException("מזהה האירוע לא תקין.");
        }
    }

    private String translateLotteryError(String message) {
        if (message == null || message.isBlank()) {
            return "הפעולה מול ההגרלה נכשלה. נסו שוב.";
        }

        if (message != null && (
                message.contains("JWT") ||
                message.contains("expired") ||
                message.contains("Invalid") ||
                message.contains("Invalid session ID") ||
                message.contains("Token is missing or null") ||
                message.contains("Session is no longer active") ||
                message.contains("Invalid or expired security token")
        )) {
            return message;
        }

        if (message.contains("Member must be logged in")) {
            return "יש להתחבר כדי לבצע פעולה בהגרלה.";
        }

        if (message.contains("Lottery for event not found")) {
            return "לא נמצאה הגרלה עבור האירוע הזה.";
        }

        if (message.contains("Event ID is invalid")) {
            return "מזהה האירוע לא תקין.";
        }

        if (message.contains("Registration is closed")) {
            return "ההרשמה להגרלה סגורה.";
        }

        if (message.contains("already registered")) {
            return "כבר נרשמת להגרלה הזו.";
        }

        return "הפעולה מול ההגרלה נכשלה. נסו שוב.";
    }
    public void registerToLottery(String memberToken, Long eventId) {
            try {
                if (memberToken == null || memberToken.isBlank()) {
                    throw new PresentationException("יש להתחבר כדי להירשם להגרלה.");
                }

                validateEventId(eventId);

                lotteryService.registerMemberToLotteryByEventId(memberToken, eventId);

            } catch (PresentationException e) {
                throw e;

            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new PresentationException(translateLotteryError(e.getMessage()));

            } catch (Exception e) {
                throw new PresentationException("ההרשמה להגרלה נכשלה. נסו שוב.");
            }
        }
        public PurchaseRequestResult requestPurchase(String sessionToken, Long eventId) {
            try {
                if (sessionToken == null || sessionToken.isBlank()) {
                    throw new PresentationException("החיבור למערכת לא פעיל. רענני את הדף ונסי שוב.");
                }

                validateEventId(eventId);

                String result = waitingQueueService.tryReserve(eventId, sessionToken);

                if ("APPROVED".equals(result)) {
                    return new PurchaseRequestResult(
                            purchaseRoute(eventId),
                            "אפשר להמשיך לבחירת כרטיסים."
                    );
                }

                if ("QUEUED".equals(result)) {
                    return new PurchaseRequestResult(
                            waitingQueueRoute(eventId),
                            "האירוע עמוס כרגע, הועברת לתור ההמתנה."
                    );
                }

                throw new PresentationException(translateQueueResult(result));

            } catch (PresentationException e) {
                throw e;

            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new PresentationException(translateQueueResult(e.getMessage()));

            } catch (Exception e) {
                throw new PresentationException("לא ניתן להתחיל רכישה כרגע. נסו שוב.");
            }
        }
        public String waitingQueueRoute(Long eventId) {
            validateEventId(eventId);
            return UiRoutes.WAITING_QUEUE.replace(":eventId", String.valueOf(eventId));
        }
        private String translateQueueResult(String message) {
            if (message == null || message.isBlank()) {
                return "לא ניתן להתחיל רכישה כרגע. נסו שוב.";
            }

            if (message != null && (
                    message.contains("JWT") ||
                    message.contains("expired") ||
                    message.contains("Invalid") ||
                    message.contains("Invalid session ID") ||
                    message.contains("Token is missing or null") ||
                    message.contains("Session is no longer active") ||
                    message.contains("Invalid or expired security token")
            )) {
                return message;
            }

            if (message.contains("Event not found")) {
                return "האירוע לא נמצא.";
            }

            if (message.contains("Sold Out")) {
                return "האירוע אזל ולא ניתן לרכוש כרטיסים.";
            }

            if (message.contains("too busy")) {
                return "המערכת עמוסה כרגע. נסו שוב בעוד רגע.";
            }

            return "לא ניתן להתחיל רכישה כרגע. נסו שוב.";
        }
        public record PurchaseRequestResult(
                String route,
                String message
        ) {
        }

}