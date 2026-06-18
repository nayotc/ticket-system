package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;

/**
 * Explicitly maps between history domain aggregates and DTOs.
 *
 * <p>The mapper prevents DTO input from controlling database-specific state
 * such as generated entity identifiers. It also keeps persistence details out
 * of DTO responses.</p>
 */
public final class HistoryMapper {

    /**
     * Utility class; instances are not required.
     */
    private HistoryMapper() {
    }

    /**
     * Creates a new purchase aggregate from a completed order.
     *
     * <p>The order's purchase identifier is intentionally ignored. A new
     * purchase receives its identifier from the database.</p>
     *
     * @param order completed order
     * @return new purchase aggregate
     */
    public static Purchase toPurchase(OrderDTO order) {
        Objects.requireNonNull(
                order,
                "Completed order cannot be null."
        );

        List<PurchaseDTO> orderTickets = order.getTickets();

        List<PurchasedTicket> tickets =
                orderTickets == null || orderTickets.isEmpty()
                        ? List.of()
                        : orderTickets.stream()
                                .map(HistoryMapper::toPurchasedTicket)
                                .toList();

        Purchase purchase = new Purchase(
                tickets,
                order.getEventName(),
                order.getLocation(),
                order.getMemberId(),
                order.getCompanyId(),
                order.getManagedByMemberId(),
                order.getEventId(),
                order.getTotalPrice(),
                order.getTransactionId()
        );

        purchase.setRefunded(order.isRefunded());

        return purchase;
    }

    /**
     * Converts a purchase aggregate to the DTO exposed by application
     * services.
     *
     * @param purchase persisted purchase
     * @return order DTO
     */
    public static OrderDTO toOrderDTO(Purchase purchase) {
        Objects.requireNonNull(
                purchase,
                "Purchase cannot be null."
        );

        List<PurchaseDTO> ticketDTOs =
                purchase.getTickets().stream()
                        .map(HistoryMapper::toPurchaseDTO)
                        .toList();

        return new OrderDTO(
                purchase.getPurchaseId(),
                ticketDTOs,
                purchase.getEventName(),
                purchase.getLocation(),
                purchase.getMemberId(),
                purchase.getCompanyId(),
                purchase.getManagedByMemberId(),
                purchase.getEventId(),
                purchase.getTotalPrice(),
                purchase.getTransactionId(),
                purchase.isRefunded()
        );
    }

    /**
     * Converts one purchased-ticket DTO to a historical snapshot.
     *
     * @param ticketDTO ticket DTO
     * @return purchased-ticket snapshot
     */
    private static PurchasedTicket toPurchasedTicket(
            PurchaseDTO ticketDTO
    ) {
        Objects.requireNonNull(
                ticketDTO,
                "Purchased ticket DTO cannot be null."
        );

        PurchasedTicket ticket = new PurchasedTicket(
                ticketDTO.getTicketId(),
                ticketDTO.getRow(),
                ticketDTO.getChair(),
                ticketDTO.getPrice(),
                ticketDTO.getSecureBarcode()
        );

        ticket.setStatus(
                parseStatus(ticketDTO.getStatus())
        );

        return ticket;
    }

    /**
     * Converts one historical ticket to a DTO.
     *
     * @param ticket purchased-ticket snapshot
     * @return ticket DTO
     */
    private static PurchaseDTO toPurchaseDTO(
            PurchasedTicket ticket
    ) {
        Objects.requireNonNull(
                ticket,
                "Purchased ticket cannot be null."
        );

        return new PurchaseDTO(
                ticket.getTicketId(),
                ticket.getRow(),
                ticket.getChair(),
                ticket.getPrice(),
                ticket.getStatus().name(),
                ticket.getSecureBarcode()
        );
    }

    /**
     * Converts the textual DTO status to the domain enum.
     *
     * <p>Older completed orders may contain a missing or blank status. Such a
     * purchased ticket is considered active at the time it enters history.</p>
     *
     * @param status textual status
     * @return domain ticket status
     */
    private static TicketStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TicketStatus.ACTIVE;
        }

        try {
            return TicketStatus.valueOf(
                    status.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported purchased ticket status: " + status,
                    exception
            );
        }
    }
}