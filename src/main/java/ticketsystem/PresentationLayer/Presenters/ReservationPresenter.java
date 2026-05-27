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
                throw new PresentationException("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw new PresentationException("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw new PresentationException("Area id is invalid.");
            }

            if (row <= 0 || chair <= 0) {
                throw new PresentationException("Seat position is invalid.");
            }

            boolean selected = reservationService.selectSeatTicket(
                    token,
                    eventId,
                    areaId,
                    new seatPositionDTO(row, chair),
                    normalizeOptionalText(lotteryCode)
            );

            if (!selected) {
                throw new PresentationException("Ticket selection failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Ticket selection failed. Please try again.");
        }
    }
    public boolean selectStandingTicket(Long eventId, Long areaId, int quantity, String lotteryCode) {
        try {
            String token = UiSession.getCurrentToken();

            if (token == null) {
                throw new PresentationException("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw new PresentationException("Event id is invalid.");
            }

            if (areaId == null || areaId <= 0) {
                throw new PresentationException("Area id is invalid.");
            }

            if (quantity <= 0) {
                throw new PresentationException("Ticket quantity must be greater than zero.");
            }

            boolean selected = reservationService.selectStandingTicket(
                    token,
                    eventId,
                    areaId,
                    quantity,
                    normalizeOptionalText(lotteryCode)
            );

            if (!selected) {
                throw new PresentationException("Ticket selection failed. Please try again.");
            }

            return true;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw new PresentationException(e.getMessage());

        } catch (Exception e) {
            throw new PresentationException("Ticket selection failed. Please try again.");
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
