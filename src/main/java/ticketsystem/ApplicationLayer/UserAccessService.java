package ticketsystem.ApplicationLayer;

import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Member;

@Service
public class UserAccessService {
    private final IUserRepository userRepository;

    public UserAccessService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateCanPerformNonViewAction(Long memberId) {
        if (memberId == null) {
            return;
        }
        Member member = userRepository.getMemberById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found");
        }

        if (member.isSuspended()) {
            throw new IllegalArgumentException(
                    "Suspended users can only perform view actions"
            );
        }   
    }
}