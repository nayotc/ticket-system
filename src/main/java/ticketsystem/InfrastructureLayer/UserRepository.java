package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.persistence.UserJpaRepository;

@Repository
public class UserRepository implements IUserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public boolean addRegisteredMember(long id, Member member, String hashedPassword) {
        if (member == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        if (userJpaRepository.existsMemberByUserName(member.getUserName())) {
            return false;
        }
        if (id != 0L && userJpaRepository.existsById(id)) {
            return false;
        }
        member.setHashedPassword(hashedPassword);
        Member saved = userJpaRepository.save(member);
        return saved.getId() != null;
    }

    @Override
    @Transactional
    public boolean removeRegisteredMember(long id) {
        if (!userJpaRepository.existsById(id)) {
            return false;
        }
        userJpaRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIDTaken(long id) {
        return userJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        return userJpaRepository.existsMemberByUserName(username);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAllRegisteredMembersCount() {
        return userJpaRepository.findAllMembers().size();
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByUsername(String username) {
        return userJpaRepository.findMemberByUserName(username)
                .map(Member::new)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberById(long id) {
        return userJpaRepository.findById(id)
                .filter(Member.class::isInstance)
                .map(user -> new Member((Member) user))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getHashedPasswordByUsername(String username) {
        return userJpaRepository.findMemberByUserName(username)
                .map(Member::getHashedPassword)
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean updateRegisteredMemberUsername(String username, String newUsername) {
        return userJpaRepository.findMemberByUserName(username)
                .map(member -> {
                    if (newUsername == null || newUsername.isBlank()) {
                        return false;
                    }
                    if (!username.equals(newUsername) && userJpaRepository.existsMemberByUserName(newUsername)) {
                        return false;
                    }
                    member.setUserName(newUsername);
                    userJpaRepository.save(member);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean updateRegisteredMemberPassword(String username, String newHashedPassword) {
        return userJpaRepository.findMemberByUserName(username)
                .map(member -> {
                    if (newHashedPassword == null || newHashedPassword.isBlank()) {
                        return false;
                    }
                    member.setHashedPassword(newHashedPassword);
                    userJpaRepository.save(member);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean updateMember(Member targetMember) {
        if (targetMember == null || targetMember.getId() == null) {
            return false;
        }
        if (!userJpaRepository.existsById(targetMember.getId())) {
            return false;
        }
        Member merged = new Member(targetMember);
        try {
            userJpaRepository.save(merged);
            return true;
        } catch (OptimisticLockingFailureException ex) {
            throw new RuntimeException("OptimisticLockingFailureException: Concurrent modification detected for member "
                    + targetMember.getId(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return userJpaRepository.findAllMembers().stream()
                .map(Member::new)
                .collect(Collectors.toList());
    }
}
