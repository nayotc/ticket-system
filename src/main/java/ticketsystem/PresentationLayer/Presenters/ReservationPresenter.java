package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.EventMapDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.EventTicketSelectionDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.MapElementDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.MapElementTypeDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.MapPositionDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatStatusDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.SeatingAreaDto;
import ticketsystem.PresentationLayer.DTO.TicketSelectionViewModel.StandingAreaDto;
import ticketsystem.DTO.ActiveOrderDTO;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReservationPresenter {

    private final ReservationService reservationService;
    private final EventService eventService;

    public ReservationPresenter(ReservationService reservationService, EventService eventService) {
        this.reservationService = reservationService;
        this.eventService = eventService;
    }

    /**
     * Loads the current active order for the active UI session.
     *
     * This method is used by presentation-layer views such as the active order cart.
     * A missing active order is treated as an empty-cart state rather than an error,
     * so the service may return null and the view can render its empty state.
     *
     * @param token active guest/member session token
     * @return current active order DTO, or null if no active order exists
     */
    public ActiveOrderDTO loadActiveOrder(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            return reservationService.viewCurrentActiveOrder(token);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Active order could not be loaded. Please try again.");
        }
    }

    public EventDTO loadEvent(String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            EventDTO event = eventService.getEvent(token, eventId);

            if (event == null || event.map() == null) {
                throw presentationError("Event map is not available.");
            }

            return event;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Event data could not be loaded. Please try again.");
        }
    }

    public EventMapDTO loadEventMap (String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            EventMapDTO MapDTO= eventService.getEventMap(token, eventId);

            if (MapDTO == null) {
                throw presentationError("Event map is not available.");
            }

            return MapDTO;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("Event data could not be loaded. Please try again.");
        }
    }

    public boolean selectSeatTicket(String token, Long eventId, Long areaId, int row, int chair, String lotteryCode){
        try {
            if (token == null || token.isBlank()) {
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

    /**
     * Removes a selected ticket from the current active order.
     *
     * This method is used by active-order presentation flows where the UI already
     * has the active order event id and the selected ticket id.
     *
     * @param token active guest/member session token
     * @param eventId event whose active order contains the ticket
     * @param ticketId selected ticket to remove from the active order
     * @return true if the ticket was removed successfully
     */
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            if (ticketId == null || ticketId <= 0) {
                throw presentationError("Ticket id is invalid.");
            }

            boolean removed = reservationService.removeTicketFromActiveOrder(
                    token,
                    eventId,
                    ticketId
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

    public boolean removeSeatTicketFromActiveOrder(String token, Long eventId, Long areaId, int row, int chair){
        try {
            if (token == null || token.isBlank()) {
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

    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity, String lotteryCode){
        try {

            if (token == null || token.isBlank()) {
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

    public boolean removeStandingTicketsFromActiveOrder(String token, Long eventId, Long areaId, int quantity){
        try {
            if (token == null || token.isBlank()) {
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

    private EventTicketSelectionDto toTicketSelectionDto(EventDTO event) {
        EventMapDTO map = event.map();

        return new EventTicketSelectionDto(
                String.valueOf(event.id()),
                event.name(),
                event.date(),
                formatLocation(event.location()),
                new EventMapDto(
                        safeInt(map.size(), true, 1),  // rows = height
                        safeInt(map.size(), false, 1), // columns = width
                        toMapElements(map, event.ticketPrice())
                )
        );
    }

    private List<MapElementDto> toMapElements(EventMapDTO map, BigDecimal defaultTicketPrice) {
        if (map == null || map.elements() == null) {
            return List.of();
        }

        List<MapElementDto> elements = new ArrayList<>();

        for (IMapElementDTO element : map.elements()) {
            if (element instanceof SeatingAreaDTO seatingArea) {
                elements.add(toSeatingAreaDto(seatingArea, defaultTicketPrice));

            } else if (element instanceof StandingAreaDTO standingArea) {
                elements.add(toStandingAreaDto(standingArea, defaultTicketPrice));

            } else if (element instanceof ElementDTO plainElement) {
                elements.add(toPlainMapElementDto(plainElement));
            }
        }

        return elements;
    }

    private SeatingAreaDto toSeatingAreaDto(SeatingAreaDTO area, BigDecimal defaultTicketPrice) {
        List<SeatDto> seats = area.seats() == null
                ? List.of()
                : area.seats().stream()
                .map(this::toSeatDto)
                .toList();

        return new SeatingAreaDto(
                area.id(),
                area.name(),
                toMapPosition(area.location(), area.size()),
                priceOrZero(defaultTicketPrice),
                area.rows(),
                area.columns(),
                seats
        );
    }

    private StandingAreaDto toStandingAreaDto(StandingAreaDTO area, BigDecimal defaultTicketPrice) {
        return new StandingAreaDto(
                area.id(),
                area.name(),
                toMapPosition(area.location(), area.size()),
                priceOrZero(defaultTicketPrice),
                safeLongToInt(area.capacity()),
                safeLongToInt(area.reserved()),
                safeLongToInt(area.sold())
        );
    }

    private MapElementDto toPlainMapElementDto(ElementDTO element) {
        return new MapElementDto(
                element.id(),
                element.name(),
                toMapElementType(element),
                toMapPosition(element.location(), element.size())
        );
    }

    private SeatDto toSeatDto(SeatDTO seat) {
        int row = seat.position() == null ? 1 : seat.position().row();
        int number = seat.position() == null ? 1 : seat.position().number();

        return new SeatDto(
                row,
                number,
                toSeatStatus(seat.status())
        );
    }

    private MapPositionDto toMapPosition(PairDTO<Integer, Integer> location, PairDTO<Integer, Integer> size) {
        return new MapPositionDto(
                location == null || location.first() == null ? 1 : location.first(),
                location == null || location.second() == null ? 1 : location.second(),
                size == null || size.first() == null ? 1 : size.first(),
                size == null || size.second() == null ? 1 : size.second()
        );
    }

    private MapElementTypeDto toMapElementType(ElementDTO element) {
        String type = element.type() == null ? "" : element.type().toUpperCase();
        String name = element.name() == null ? "" : element.name();

        if (type.contains("STAGE") || name.contains("במה") || name.equalsIgnoreCase("stage")) {
            return MapElementTypeDto.STAGE;
        }

        if (type.contains("ENTRANCE") || name.contains("כניסה") || name.equalsIgnoreCase("entrance")) {
            return MapElementTypeDto.ENTRANCE;
        }

        if (type.contains("EXIT") || name.contains("יציאה") || name.equalsIgnoreCase("exit")) {
            return MapElementTypeDto.EXIT;
        }

        return MapElementTypeDto.GENERIC;
    }

    private SeatStatusDto toSeatStatus(String status) {
        if (status == null || status.isBlank()) {
            return SeatStatusDto.AVAILABLE;
        }

        try {
            return SeatStatusDto.valueOf(status);
        } catch (IllegalArgumentException e) {
            return SeatStatusDto.AVAILABLE;
        }
    }

    private String formatLocation(String location) {
        return location == null || location.isBlank() ? "מיקום לא זמין" : location.replace("_", " ");
    }

    private BigDecimal priceOrZero(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private int safeInt(PairDTO<Integer, Integer> pair, boolean first, int fallback) {
        if (pair == null) {
            return fallback;
        }

        Integer value = first ? pair.first() : pair.second();
        return value == null ? fallback : value;
    }

    private int safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
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
            case "Event map is not available.",
                 "Event data could not be loaded. Please try again." ->
                    "לא ניתן לטעון את מפת האירוע. יש לנסות שוב.";
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
            case "Ticket removal failed. Please try again." ->
                    "הסרת הכרטיסים נכשלה. יש לנסות שוב.";
            default ->
                    "בחירת הכרטיסים נכשלה. יש לנסות שוב.";
        };
    }
}
