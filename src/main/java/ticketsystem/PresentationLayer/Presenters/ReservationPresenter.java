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
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.MyAccountDTO;
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
import ticketsystem.DTO.TicketDTO;
import ticketsystem.PresentationLayer.DTO.AppliedDiscount;
import ticketsystem.PresentationLayer.DTO.OrderEventInfo;
import ticketsystem.PresentationLayer.DTO.OrderPricing;
import ticketsystem.DTO.AppliedDiscountDTO;
import ticketsystem.DTO.PricingQuoteDTO;
import ticketsystem.ApplicationLayer.WaitingQueueService;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReservationPresenter {

    private final ReservationService reservationService;
    private final EventService eventService;
    private final UserService userService;
    private final WaitingQueueService waitingQueueService;

    public ReservationPresenter(ReservationService reservationService, EventService eventService, UserService userService, WaitingQueueService waitingQueueService) {
        this.reservationService = reservationService;
        this.eventService = eventService;
        this.userService = userService;
        this.waitingQueueService=waitingQueueService;
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Active order could not be loaded. Please try again.");
        }
    }

    /**
     * Loads the logged-in buyer's profile details for checkout prefill.
     *
     * This method is used by the checkout view only when the current UI session
     * belongs to a logged-in member. It retrieves the member profile from
     * UserService using the member session token, so checkout can prefill buyer
     * fields such as full name, email and phone.
     *
     * Guest sessions should not call this method, because guests do not have
     * member profile details.
     *
     * @param token active member session token
     * @return buyer profile details for the logged-in member
     * @throws PresentationException if the token is missing, invalid, belongs to
     *                               a non-member session, or the profile cannot be loaded
     */
    public MyAccountDTO loadBuyerDetails(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw checkoutError("No active session found. Please refresh and try again.");
            }

            return userService.getMyAccountDTO(token);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw checkoutError(e, e.getMessage());

        } catch (Exception e) {
            throw checkoutError(e, "Buyer details could not be loaded. Please try again.");
        }
    }

    /**
     * Returns the number of tickets in the current active order.
     *
     * This method is intended for lightweight presentation components such as
     * the public header cart badge. If there is no active order, the badge should
     * display zero.
     *
     * @param token active guest/member session token
     * @return number of tickets in the current active order, or zero if none exists
     */
    public int getActiveCartItemsCount(String token) {
        ActiveOrderDTO activeOrder = loadActiveOrder(token);

        if (activeOrder == null || activeOrder.getTickets() == null) {
            return 0;
        }

        return activeOrder.getTickets().size();
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Event data could not be loaded. Please try again.");
        }
    }

    /**
     * Loads basic event details for presentation flows that do not require an event map.
     *
     * This method is intended for views such as the active order cart, where the UI
     * needs event information like name, date and location, but does not need the
     * seating/standing map. Unlike loadEvent, this method does not fail when the
     * event map is missing.
     *
     * @param token active guest/member session token
     * @param eventId event identifier
     * @return event DTO with basic event details
     */
    public EventDTO loadEventDetails(String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            EventDTO event = eventService.getEvent(token, eventId);

            if (event == null) {
                throw presentationError("Event not found");
            }

            return event;

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Event data could not be loaded. Please try again.");
        }
    }

    /**
     * Loads presentation-ready event information for the active order cart.
     *
     * The active order cart needs only basic event details, without requiring
     * an event map. This method converts the application-layer EventDTO into
     * text that can be displayed directly by the view.
     *
     * @param token active guest/member session token
     * @param eventId event identifier from the active order
     * @return presentation DTO with event name, date text and location text
     */
    public OrderEventInfo loadActiveOrderEventInfo(String token, Long eventId) {
        EventDTO event = loadEventDetails(token, eventId);

        return new OrderEventInfo(
                event.name() == null || event.name().isBlank() ? "אירוע ללא שם" : event.name(),
                event.date() == null ? "תאריך יעודכן בהמשך" : event.date().toString(),
                event.location() == null || event.location().isBlank()
                        ? "מיקום יעודכן בהמשך"
                        : event.location().replace("_", " ")
        );
    }
    /**
     * Calculates the pricing summary displayed in order-related views.
     *
     * This method asks the application service for a real pricing quote of the
     * current active order. The returned DTO already includes domain-level discount
     * policy calculation, including company discounts, event discounts, coupon
     * discounts, conditional discounts, and the discount composition rule.
     *
     * The presenter maps the application-layer pricing DTO into the
     * presentation-layer OrderPricing DTO used by the UI. The view should only
     * display the result and should not calculate discounts by itself.
     *
     * @param token active guest/member session token
     * @param activeOrder active order DTO currently displayed in the UI
     * @param couponCode optional coupon code entered by the user
     * @return presentation DTO with subtotal, applied discounts, discount total, and final total
     */
    public OrderPricing calculatePricing(String token, ActiveOrderDTO activeOrder, String couponCode) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (activeOrder == null || activeOrder.getEventId() == null) {
                throw presentationError("No active order found");
            }

            PricingQuoteDTO quote = reservationService.calculateActiveOrderPricing(
                    token,
                    activeOrder.getEventId(),
                    normalizeOptionalText(couponCode)
            );

            List<AppliedDiscount> discounts = mapAppliedDiscounts(quote.appliedDiscounts());
            List<String> messages = new ArrayList<>();

            if (quote.discountTotal().compareTo(BigDecimal.ZERO) > 0) {
                messages.add("המחיר כולל הנחות שהופעלו לפי מדיניות ההנחות.");
            } else if (couponCode != null && !couponCode.isBlank()) {
                messages.add("הקופון אינו פעיל כרגע או שאינו תורם להנחה");
            }

            return new OrderPricing(
                    quote.subtotal(),
                    quote.discountTotal(),
                    quote.total(),
                    discounts,
                    messages
            );

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e, e.getMessage());
        } catch (Exception e) {
            throw presentationError(e, "Pricing could not be calculated. Please try again.");
        }
    }

    /**
     * Applies a coupon code to the pricing preview displayed in the UI.
     *
     * This method recalculates the active order pricing with the provided coupon
     * code. The coupon and all other discount rules are evaluated by the domain
     * logic through the application service.
     *
     * @param token active guest/member session token
     * @param activeOrder active order DTO currently displayed in the UI
     * @param couponCode coupon code entered by the user
     * @return presentation DTO with the updated pricing preview
     */
    public OrderPricing applyCoupon(String token, ActiveOrderDTO activeOrder, String couponCode) {
        return calculatePricing(token, activeOrder, couponCode);
    }

    /**
     * Maps application-layer applied discount DTOs into presentation DTOs.
     *
     * <p>The application DTO describes the business result returned by the
     * pricing service. The presentation DTO preserves the discount kind and adds
     * text formatted for display in order-related views.</p>
     *
     * @param dtoDiscounts discounts applied during application-service pricing
     *                     calculation
     * @return presentation discounts ready for UI display
     */
    private List<AppliedDiscount> mapAppliedDiscounts(
            List<AppliedDiscountDTO> dtoDiscounts
    ) {
        List<AppliedDiscount> discounts = new ArrayList<>();

        if (dtoDiscounts == null || dtoDiscounts.isEmpty()) {
            return discounts;
        }

        for (AppliedDiscountDTO discount : dtoDiscounts) {
            discounts.add(new AppliedDiscount(
                    discount.name(),
                    discount.kind(),
                    formatDiscountDescription(discount.percentage()),
                    discount.amount()
            ));
        }

        return discounts;
    }

    /**
     * Formats a discount percentage for display in order-related views.
     *
     * @param percentage discount percentage from the application pricing DTO
     * @return human-readable discount description
     */
    private String formatDiscountDescription(BigDecimal percentage) {
        if (percentage == null) {
            return "הנחה";
        }

        return percentage.stripTrailingZeros().toPlainString() + "% הנחה";
    }

    /**
     *check purchase policy before checkout
     *
     * @param token active guest/member session token
     * @param eventId event identifier of the active order
     * @param paymentDetails payment details entered in the checkout view
     */
    public boolean validateOrderPolicyBeforePayment(String token, Long eventId, PaymentDetails paymentDetails, String couponCode) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }
            reservationService.validateActiveOrderPolicy(token, eventId, paymentDetails, normalizeOptionalText(couponCode));
            return true;

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationError(e, e.getMessage());}
        // } catch (Exception e) {
        //     throw presentationError(e, "Failed to validate purchase policy. Please try again.");
        // }
    }

    /**
     * Leaves the queue-related selection turn after the user was promoted from the
     * waiting queue into the ticket-selection page.
     *
     * This method is different from releaseQueueAccess:
     * releaseQueueAccess silently releases a regular active purchasing slot, while
     * this method also triggers the existing user-facing queue-leave notification.
     *
     * @param token active guest/member session token
     * @param eventId event whose queue turn should be left
     */
    public void leavePromotedQueueTurn(String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                return;
            }

            if (eventId == null || eventId <= 0) {
                return;
            }

            waitingQueueService.leaveQueue(eventId, token);

        } catch (IllegalArgumentException e) {
            throw presentationError(e.getMessage());

        } catch (Exception e) {
            throw presentationError("לא ניתן לצאת מהתור כרגע.");
        }
    }

    /**
     * Completes checkout for the current active order.
     *
     * This method is used by the checkout view to submit the active order for
     * payment through the reservation application service. The final amount,
     * purchase-policy validation and coupon validation are handled by the service.
     *
     * @param token active guest/member session token
     * @param eventId event identifier of the active order
     * @param paymentDetails payment details entered in the checkout view
     * @param couponCode optional coupon code entered by the user
     * @return true if checkout completed successfully
     */
    public boolean checkout(String token, Long eventId, PaymentDetails paymentDetails, String couponCode) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            return reservationService.checkout(
                    token,
                    eventId,
                    paymentDetails, 
                    normalizeOptionalText(couponCode)
            );

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Checkout failed. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Event data could not be loaded. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket selection failed. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket removal failed. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket removal failed. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket selection failed. Please try again.");
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
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket removal failed. Please try again.");
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
                        toMapElements(map)
                )
        );
    }

    private List<MapElementDto> toMapElements(EventMapDTO map) {
        if (map == null || map.elements() == null) {
            return List.of();
        }

        List<MapElementDto> elements = new ArrayList<>();

        for (IMapElementDTO element : map.elements()) {
            if (element instanceof SeatingAreaDTO seatingArea) {
                elements.add(toSeatingAreaDto(seatingArea, seatingArea.price()));

            } else if (element instanceof StandingAreaDTO standingArea) {
                elements.add(toStandingAreaDto(standingArea, standingArea.price()));

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

        String dynamicTranslation = translateDynamicReservationError(message);
        if (dynamicTranslation != null) {
            return dynamicTranslation;
        }

        return translateExactReservationError(message);
    }

    private String translateDynamicReservationError(String message) {
        if (message.matches("Failed to validate active order policy: Cannot purchase more than \\d+ tickets\\.")) {
            String maxTickets = message.replaceAll("\\D+", "");
            return "לא ניתן לרכוש יותר מ-" + maxTickets + " כרטיסים לאירוע זה.";
        }

        if (message.contains("Customer does not meet the minimum age requirement of")) {
            String minAge = message.replaceAll("\\D+", "");
            return "הרכישה נכשלה: האירוע מוגבל מגיל " + minAge + " ומעלה בלבד.";
        }

        if (message.contains("Insufficient tickets purchased, minimum required:")) {
            String minTickets = message.replaceAll("\\D+", "");
            return "יש לרכוש לפחות " + minTickets + " כרטיסים כדי לבצע הזמנה לאירוע זה.";
        }

        if (message.startsWith("All rules failed:")) {
            String reasonsPart = message.substring("All rules failed:".length()).trim();
            String[] individualReasons = reasonsPart.split("; ");
            List<String> translatedReasons = new ArrayList<>();
            for (String reason : individualReasons) {
                translatedReasons.add(translateReservationError(reason.trim()));
            }
            return "הרכישה נדחתה עקב אי-עמידה בתנאי המדיניות: " + String.join(" וגם ", translatedReasons);
        }

        return null;
    }

    private String translateExactReservationError(String message) {
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
                 "Quantity must be greater than zero",
                 "Quantity must be positive" ->
                    "יש לבחור לפחות כרטיס אחד.";

            case "No active order found for this event",
                 "No active order found" ->
                    "לא ניתן להוסיף את הכרטיסים להזמנה כרגע. יש לנסות שוב.";

            case "An active order already exists for this user to another event" ->
                    "כבר קיימת לך הזמנה פעילה לאירוע אחר. כדי לבחור כרטיסים לאירוע הזה, יש להשלים את ההזמנה הנוכחית או להסיר ממנה את כל הכרטיסים.";

            case "Seat removal details are incomplete" ->
                    "לא ניתן להסיר את הכרטיס מההזמנה. פרטי ההסרה אינם תקינים.";

            case "Not enough standing tickets in the order to remove" ->
                    "לא נמצאו מספיק כרטיסי עמידה להסרה מההזמנה.";

            case "Suspended users can only perform view actions" ->
                    "משתמש מושהה יכול לצפות במידע בלבד ולא לבצע פעולות רכישה.";

            case "User is not allowed to view this order" ->
                    "אין לך הרשאה לצפות בהזמנה הזו.";

            case "No active order or event found",
                 "No active order with tickets" ->
                    "לא נמצאה הזמנה פעילה עם כרטיסים.";

            case "Active order has expired" ->
                    "פג תוקף שריון הכרטיסים. הכרטיסים שוחררו ויש לבחור אותם מחדש.";

            case "Ticket quantity exceeds limit" ->
                    "כמות הכרטיסים חורגת מהמגבלה המותרת להזמנה.";

            case "Order is not active" ->
                    "ההזמנה כבר אינה פעילה.";

            case "Ticket event ID does not match order event ID" ->
                    "לא ניתן להוסיף להזמנה כרטיסים מאירוע אחר.";

            case "Ticket selection failed. Please try again." ->
                    "בחירת הכרטיסים נכשלה. יש לנסות שוב.";

            case "Ticket removal failed. Please try again." ->
                    "הסרת הכרטיסים נכשלה. יש לנסות שוב.";

            case "Lottery code is required for this event" ->
                    "נדרש קוד זכייה בהגרלה כדי לבחור כרטיסים לאירוע הזה.";

            case "Invalid lottery code",
                 "Invalid winner code",
                 "Lottery code is invalid" ->
                    "קוד ההגרלה אינו תקין.";

            case "Payment service is unavailable" ->
                    "שירות התשלום אינו זמין כרגע. יש לנסות שוב מאוחר יותר.";

            case "Ticket issuing service is unavailable" ->
                    "שירות הנפקת הכרטיסים אינו זמין כרגע. יש לנסות שוב מאוחר יותר.";
            case "Selection access time could not be loaded." ->
                    "לא ניתן לטעון את זמן הגישה לבחירת הכרטיסים.";

            case "Selection access could not be checked." ->
                    "לא ניתן לבדוק את תוקף הגישה לבחירת הכרטיסים.";

            default ->
                    "בחירת הכרטיסים נכשלה. יש לנסות שוב.";
        };
    }

    private PresentationException checkoutError(String message) {
        return new PresentationException(translateCheckoutError(message));
    }

    private String translateCheckoutError(String message) {
        if (message == null || message.isBlank()) {
            return "הרכישה לא הושלמה. יש לנסות שוב.";
        }

        return switch (message) {
            case "No active session found. Please refresh and try again." ->
                    "לא נמצאה פעילות משתמש. יש לרענן את העמוד ולנסות שוב.";

            case "User is not logged in." ->
                    "יש להתחבר כדי להשלים את הפעולה.";

            case "Member not found." ->
                    "לא ניתן היה למצוא את פרטי המשתמש המחובר.";

            case "Buyer details could not be loaded. Please try again." ->
                    "לא ניתן היה לטעון את פרטי המשתמש. יש לנסות שוב.";

            case "Failed to get member DTO" ->
                    "טעינת פרטי המשתמש נכשלה. יש לנסות שוב.";

            case "No active order found for this event" ->
                    "לא נמצאה הזמנה פעילה לאירוע הזה.";

            case "No active order found" ->
                    "לא נמצאה הזמנה פעילה.";

            case "No active order or event found" ->
                    "לא נמצאה הזמנה פעילה או שלא ניתן למצוא את האירוע.";

            case "No active order with tickets" ->
                    "לא נמצאה הזמנה פעילה עם כרטיסים.";

            case "Active order has expired" ->
                    "פג תוקף שריון הכרטיסים. הכרטיסים שוחררו ויש לבחור אותם מחדש.";

            case "Active order could not be loaded. Please try again." ->
                    "טעינת ההזמנה הפעילה נכשלה. יש לנסות שוב.";

            case "User is not allowed to view this order" ->
                    "אין לך הרשאה לצפות בהזמנה הזו.";

            case "Payment details are missing" ->
                    "חסרים פרטי תשלום.";

            case "Payment details are invalid" ->
                    "פרטי התשלום אינם תקינים.";

            case "Payment method is invalid" ->
                    "אמצעי התשלום אינו תקין.";

            case "Payment failed" ->
                    "התשלום נכשל. יש לבדוק את פרטי התשלום ולנסות שוב.";

            case "Checkout failed. Please try again." ->
                    "התשלום לא הושלם. יש לנסות שוב.";

            case "Checkout could not be completed. Please try again." ->
                    "הרכישה לא הושלמה. יש לנסות שוב.";

            case "Order checkout failed" ->
                    "השלמת ההזמנה נכשלה. יש לנסות שוב.";

            case "Failed to complete checkout" ->
                    "לא ניתן היה להשלים את הרכישה. יש לנסות שוב.";

            case "Purchase could not be completed" ->
                    "הרכישה לא הושלמה. יש לנסות שוב.";

            case "Order is not active" ->
                    "ההזמנה כבר אינה פעילה.";

            default ->
                    "הרכישה לא הושלמה. יש לנסות שוב.";
        };
    }

    /**
     * Requests access to the ticket-selection page for the given event.
     *
     * This method protects the ticket-selection route itself, so users cannot
     * bypass the waiting queue by typing the ticket-selection URL directly.
     *
     * @param token active guest/member session token
     * @param eventId event whose ticket-selection page is being requested
     * @return true if the user may enter ticket selection, false if the user was placed in the waiting queue
     */
    public boolean requestTicketSelectionAccess(String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                throw presentationError("No active session found. Please refresh and try again.");
            }

            if (eventId == null || eventId <= 0) {
                throw presentationError("Event id is invalid.");
            }

            String result = waitingQueueService.tryReserve(eventId, token);

            if ("APPROVED".equals(result)) {
                return true;
            }

            if ("QUEUED".equals(result)) {
                return false;
            }

            throw presentationError(result);

        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationError(e, e.getMessage());

        } catch (Exception e) {
            throw presentationError(e, "Ticket selection failed. Please try again.");
        }
    }

    /**
     * Releases the user's active purchasing slot for an event.
     *
     * This method is called only when the purchasing flow ends explicitly:
     * after successful checkout or when the user cancels checkout.
     *
     * Queue cleanup is best-effort and must not turn an already completed payment
     * into a presentation-layer failure.
     *
     * @param token active guest/member session token
     * @param eventId event whose purchasing slot should be released
     */
    public void releaseQueueAccess(String token, Long eventId) {
        try {
            if (token == null || token.isBlank()) {
                return;
            }

            if (eventId == null || eventId <= 0) {
                return;
            }

            waitingQueueService.releaseSpot(eventId, token);

        } catch (Exception ignored) {
            /*
            * A successful checkout must remain successful even if queue cleanup
            * encounters a temporary failure.
            */
        }
    }
    public long getSelectionAccessSecondsLeft(String token, Long eventId) {
    try {
        if (token == null || token.isBlank()) {
            throw presentationError("No active session found. Please refresh and try again.");
        }

        if (eventId == null || eventId <= 0) {
            throw presentationError("Event id is invalid.");
        }

        return waitingQueueService.getSelectionAccessSecondsLeft(eventId, token);

    } catch (PresentationException e) {
        throw e;

    } catch (Exception e) {
        throw presentationError(e, "Selection access time could not be loaded.");
    }
}

public boolean expireSelectionAccessIfNeeded(String token, Long eventId) {
    try {
        if (token == null || token.isBlank()) {
            throw presentationError("No active session found. Please refresh and try again.");
        }

        if (eventId == null || eventId <= 0) {
            throw presentationError("Event id is invalid.");
        }

        return waitingQueueService.expireSelectionAccessIfNeeded(eventId, token);

    } catch (PresentationException e) {
        throw e;

    } catch (Exception e) {
        throw presentationError(e, "Selection access could not be checked.");
    }
}

    private PresentationException presentationError(Exception e, String fallbackMessage) {
        return PresentationException.dispatch(e, msg -> translateReservationError(msg != null ? msg : fallbackMessage));
    }

    private PresentationException checkoutError(Exception e, String fallbackMessage) {
        return PresentationException.dispatch(e, msg -> translateCheckoutError(msg != null ? msg : fallbackMessage));
    }

}
