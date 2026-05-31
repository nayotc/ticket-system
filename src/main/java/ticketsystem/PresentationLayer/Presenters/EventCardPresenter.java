package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

@Component
public class EventCardPresenter {

    private final LotteryService lotteryService;

    public EventCardPresenter(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
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

        if (message.contains("token")
                || message.contains("Token")
                || message.contains("session")
                || message.contains("Session")
                || message.contains("security token")) {
            return "החיבור למערכת לא תקין. התחברי מחדש ונסי שוב.";
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
}