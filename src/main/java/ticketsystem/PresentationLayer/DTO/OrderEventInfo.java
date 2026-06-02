package ticketsystem.PresentationLayer.DTO;

/**
 * Presentation DTO with basic event details displayed in order-related views.
 *
 * This object contains text prepared for the UI, such as the event name,
 * formatted date, and formatted location. It can be used by views such as
 * the active order cart and checkout.
 *
 * @param eventName event name to display
 * @param dateText formatted event date text
 * @param locationText formatted event location text
 */
public record OrderEventInfo(
        String eventName,
        String dateText,
        String locationText
) {
}
