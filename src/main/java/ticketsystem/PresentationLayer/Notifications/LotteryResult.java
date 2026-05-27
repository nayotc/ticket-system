package ticketsystem.PresentationLayer.Notifications;

import ticketsystem.PresentationLayer.Constants.UiRoutes;

public record LotteryResult(
        String lotteryId,
        String eventId,
        String eventName,
        String purchaseCode
) {
    public LotteryResult {
        eventName = safeText(eventName, "האירוע שלך");
        purchaseCode = safeText(purchaseCode, "");
    }

    public String purchaseRoute() {
        if (hasText(lotteryId)) {
            return UiRoutes.LOTTERY_RESULT_CODE.replace(":lotteryId", lotteryId.trim());
        }

        if (hasText(eventId)) {
            return UiRoutes.TICKET_SELECTION.replace(":eventId", eventId.trim());
        }

        return UiRoutes.EVENTS;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeText(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }

        return value.trim();
    }
}
