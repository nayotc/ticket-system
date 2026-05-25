package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.INotifier;

public class VaadinNotifier implements INotifier {

    private final Broadcaster broadcaster;
    private final NotificationsRepository notificationRepository;

    public VaadinNotifier(Broadcaster broadcaster, NotificationsRepository notificationRepository) {
        this.broadcaster = broadcaster;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void notifyUser(String sessionId, String message) {
        // user logged in:send notification immediately to user
        Broadcaster.broadcast(sessionId, message);
    }

    public void notifyMember(Long memberId, String message) {
        String sessionId = Long.toString(memberId);
        if (!broadcaster.broadcast(sessionId, message)) {
            // user is offline: save notification for later delivery
            notificationRepository.save(memberId, message);
        }
    }

}
