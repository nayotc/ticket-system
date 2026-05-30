package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.PresentationLayer.Session.UiSession;

@Component
public class ReservationPresenter {

    private final ReservationService reservationService;

    public ReservationPresenter(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public boolean selectSeatTicket(Long eventId, Long areaId, int row, int chair, String lotteryCode) {
        try {
            String token = UiSession.getCurrentToken();

            if (token == null) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw presentationError("Area id is invalid.");
            }

            if (row <= 0 || chair <= 0) {
                throw presentationError("Seat position is invalid.");
            }

            boolean selected = reservationService.selectSeatTicket(
                    token,
                    eventId,
                    areaId,
                    new seatPositionDTO(row, chair),
                    normalizeOptionalText(lotteryCode)
            );

            if (!selected) {
                throw presentationError("Ticket selection failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Ticket selection failed. Please try again.");
        }
    }

    public boolean removeSeatTicketFromActiveOrder(Long eventId, Long areaId, int row, int chair) {
        try {
            String token = UiSession.getCurrentToken();

            if (token == null) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw presentationError("Area id is invalid.");
            }

            if (row <= 0 || chair <= 0) {
                throw presentationError("Seat position is invalid.");
            }

            boolean removed = reservationService.removeSeatTicketFromActiveOrder(
                    token,
                    eventId,
                    areaId,
                    new seatPositionDTO(row, chair)
            );

            if (!removed) {
                throw presentationError("Ticket removal failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Ticket removal failed. Please try again.");
        }
    }


    public boolean selectStandingTicket(Long eventId, Long areaId, int quantity, String lotteryCode) {
        try {
            String token = UiSession.getCurrentToken();

            if (token == null) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw presentationError("Area id is invalid.");
            }

            if (quantity <= 0) {
                throw presentationError("Ticket quantity must be greater than zero.");
            }

            boolean selected = reservationService.selectStandingTicket(
                    token,
                    eventId,
                    areaId,
                    quantity,
                    normalizeOptionalText(lotteryCode)
            );

            if (!selected) {
                throw presentationError("Ticket selection failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Ticket selection failed. Please try again.");
        }
    }

    public boolean removeStandingTicketsFromActiveOrder(Long eventId, Long areaId, int quantity) {
        try {
            String token = UiSession.getCurrentToken();

            if (token == null) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw presentationError("Area id is invalid.");
            }

            if (quantity <= 0) {
                throw presentationError("Ticket quantity must be greater than zero.");
            }

            boolean removed = reservationService.removeStandingTicketsFromActiveOrder(
                    token,
                    eventId,
                    areaId,
                    quantity
            );

            if (!removed) {
                throw presentationError("Ticket removal failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Ticket removal failed. Please try again.");
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PresentationException presentationError(String message) {
        return new PresentationException(translateReservationError(message));
    }

    private String translateReservationError(String message) {
        if (message == null || message.isBlank()) {
            return "בחירת הכרטיסים נכשלה. יש לנסות שוב.";
        }

        return switch (message) {
            case "No active session found. Please refresh and try again." ->
                    "לא נמצאה פעילות משתמש. יש לרענן את העמוד ולנסות שוב.";
            case "Event id is invalid.", "Event not found" ->
                    "לא ניתן למצוא את האירוע המבוקש.";
            case "Area id is invalid." ->
                    "לא ניתן לבחור כרטיסים באזור זה.";
            case "Seat position is invalid." ->
                    "לא ניתן לבחור את המושב המבוקש.";
            case "Ticket quantity must be greater than zero.",
                 "Quantity must be greater than zero" ->
                    "יש לבחור לפחות כרטיס אחד.";
            case "No active order found for this event",
                 "No active order found" ->
                    "לא ניתן להוסיף את הכרטיסים להזמנה כרגע. יש לנסות שוב.";
            case "Ticket selection failed. Please try again." ->
                    "בחירת הכרטיסים נכשלה. יש לנסות שוב.";
            default ->
                    "בחירת הכרטיסים נכשלה. יש לנסות שוב.";
        };
    }
}
