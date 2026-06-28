package ticketsystem.InfrastructureLayer;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.DomainLayer.notifications.Notification.Type;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class VaadinNotifier implements INotifier {

    private final INotificationsRepository notificationsRepository;

    public VaadinNotifier(INotificationsRepository notificationsRepository) {
        this.notificationsRepository = notificationsRepository;
    }

    @Override
    @Transactional
    public void notifyMember(Long memberId, String message) {
        if (memberId == null || message == null || message.isBlank()) {
            return;
        }

        String targetId = memberId.toString();
        boolean hasListeners = Broadcaster.hasListeners(targetId);

        if ("Your active order is about to expire. Please complete your purchase soon.".equals(message.trim())
                && !hasListeners) {
            return;
        }

        Notification notification = new Notification(targetId, message, Type.INFO);

        Notification savedNotification = notificationsRepository.save(notification);

        broadcastAfterCommit(targetId, savedNotification);
    }

    @Override
    public void notifyGuest(String sessionId, String message) {
        if (sessionId == null || sessionId.isBlank() || message == null || message.isBlank()) {
            return;
        }

        Notification notification = new Notification(sessionId, message, Type.INFO);

        broadcastAfterCommit(sessionId, notification);
    }

    @Override
    public void notifyMembers(Collection<Long> memberIds, String message) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        for (Long memberId : memberIds) {
            if (memberId != null) {
                notifyMember(memberId, message);
            }
        }
    }

    @Override
    public void notifyGuests(Collection<String> guestTokens, String message) {
        if (guestTokens == null || guestTokens.isEmpty()) {
            return;
        }

        for (String guestToken : guestTokens) {
            if (guestToken != null && !guestToken.isBlank()) {
                notifyGuest(guestToken, message);
            }
        }

    }

    @Override
    @Transactional
    public void notifyMemberAssignment(Long memberId, String message, Long companyId) {
        if (memberId == null || message == null || message.isBlank() || companyId == null) {
            return;
        }

        String targetId = memberId.toString();
        Notification notification = new Notification(targetId, message, companyId, Type.ACTION);

        Notification savedNotification = notificationsRepository.save(notification);

        broadcastAfterCommit(targetId, savedNotification);
        }

    @Override
    public void notifyMemberIfOnline(Long memberId, String message) {
        if (memberId == null || message == null || message.isBlank()) {
            return;
        }

        String targetId = memberId.toString();

        if (!Broadcaster.hasListeners(targetId)) {
            return;
        }

        Notification notification = new Notification(targetId, message, Type.INFO);
        Broadcaster.broadcast(targetId, notification);
    }

    private void broadcastAfterCommit(String targetId, Notification notification) {
        boolean activeTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();

        if (activeTransaction && synchronizationActive) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            Broadcaster.broadcast(targetId, notification);
                        }
                    }
            );

            return;
        }

        Broadcaster.broadcast(targetId, notification);
    }
}
