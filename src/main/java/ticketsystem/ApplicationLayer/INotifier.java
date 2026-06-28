package ticketsystem.ApplicationLayer;

import java.util.Collection;

public interface INotifier {

    void notifyMember(Long memberId, String message);

    default void notifyMemberAssignment(Long memberId, String message, Long companyId) {
        notifyMember(memberId, message);
    }

    void notifyGuest(String guestToken, String message);

    void notifyMembers(Collection<Long> memberIds, String message);

    void notifyGuests(Collection<String> guestTokens, String message);

    default void notifyMemberIfOnline(Long memberId, String message) {
        notifyMember(memberId, message);
    }
}
