package ticketsystem.testutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ticketsystem.ApplicationLayer.INotifier;

public class RecordingNotifier implements INotifier {

    public record NotificationCall(String channel, String targetId, String message) {}

    private final List<NotificationCall> calls = new ArrayList<>();

    @Override
    public void notifyMember(Long memberId, String message) {
        if (memberId == null || message == null || message.isBlank()) {
            return;
        }
        calls.add(new NotificationCall("member", memberId.toString(), message));
    }

    @Override
    public void notifyGuest(String guestToken, String message) {
        if (guestToken == null || guestToken.isBlank() || message == null || message.isBlank()) {
            return;
        }
        calls.add(new NotificationCall("guest", guestToken, message));
    }

    @Override
    public void notifyMembers(Collection<Long> memberIds, String message) {
        if (memberIds == null) {
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
        if (guestTokens == null) {
            return;
        }
        for (String guestToken : guestTokens) {
            if (guestToken != null && !guestToken.isBlank()) {
                notifyGuest(guestToken, message);
            }
        }
    }

    @Override
    public void notifyMemberAssignment(Long memberId, String message, Long companyId) {
        notifyMember(memberId, message);
    }

    public List<NotificationCall> getCalls() {
        return List.copyOf(calls);
    }

    public int size() {
        return calls.size();
    }

    public void clear() {
        calls.clear();
    }

    public boolean containsMessage(String text) {
        return calls.stream().anyMatch(call -> call.message().contains(text));
    }

    public void assertNotifiedMember(long memberId, String messageFragment) {
        boolean found = calls.stream()
                .anyMatch(call -> "member".equals(call.channel())
                        && call.targetId().equals(Long.toString(memberId))
                        && call.message().contains(messageFragment));
        assertTrue(found,
                "Expected member " + memberId + " to be notified with message containing: " + messageFragment
                        + ". Actual calls: " + calls);
    }

    public void assertNotifiedGuest(String token, String messageFragment) {
        boolean found = calls.stream()
                .anyMatch(call -> "guest".equals(call.channel())
                        && call.targetId().equals(token)
                        && call.message().contains(messageFragment));
        assertTrue(found,
                "Expected guest " + token + " to be notified with message containing: " + messageFragment
                        + ". Actual calls: " + calls);
    }

    public void assertNoNotifications() {
        assertEquals(0, calls.size(), "Expected no notifications but got: " + calls);
    }

    public void assertNotificationCount(int expected) {
        assertEquals(expected, calls.size(), "Unexpected notification count. Actual calls: " + calls);
    }
}
