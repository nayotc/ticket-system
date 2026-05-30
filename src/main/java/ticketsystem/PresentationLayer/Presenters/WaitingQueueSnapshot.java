package ticketsystem.PresentationLayer.Presenters;

public record WaitingQueueSnapshot(
        String eventName,
        int position,
        int estimatedWaitMinutes,
        WaitingQueueStatus status,
        int progressPercent,
        String message
) {

    public static WaitingQueueSnapshot waiting(String eventName, int position, int estimatedWaitMinutes) {
        int progress = estimateProgress(position);
        return new WaitingQueueSnapshot(
                eventName,
                position,
                estimatedWaitMinutes,
                WaitingQueueStatus.MOVING,
                progress,
                "המערכת מכניסה ממתינים בהדרגה כדי לשמור על יציבות בזמן עומס."
        );
    }

    public static WaitingQueueSnapshot ready(String eventName) {
        return new WaitingQueueSnapshot(
                eventName,
                0,
                0,
                WaitingQueueStatus.READY,
                100,
                "התור שלך הגיע. אפשר להיכנס עכשיו לבחירת כרטיסים."
        );
    }

    public static WaitingQueueSnapshot soldOut(String eventName) {
        return new WaitingQueueSnapshot(
                eventName,
                0,
                0,
                WaitingQueueStatus.SOLD_OUT,
                0,
                "האירוע אזל. התור נסגר ולא ניתן להמשיך לרכישה."
        );
    }

    public static WaitingQueueSnapshot error(String eventName, String message) {
        return new WaitingQueueSnapshot(
                eventName,
                0,
                0,
                WaitingQueueStatus.ERROR,
                0,
                message == null || message.isBlank() ? "אירעה שגיאה בטעינת מצב התור" : message
        );
    }

    private static int estimateProgress(int position) {
        if (position <= 1) {
            return 95;
        }
        if (position <= 20) {
            return 82;
        }
        if (position <= 100) {
            return 68;
        }
        if (position <= 500) {
            return 48;
        }
        return 28;
    }
}
