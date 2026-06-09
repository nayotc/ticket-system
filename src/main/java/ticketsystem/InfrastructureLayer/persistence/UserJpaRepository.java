package ticketsystem.InfrastructureLayer.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    @Query("SELECT m FROM Member m WHERE m.userName = :username")
    Optional<Member> findMemberByUserName(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.userName = :username")
    boolean existsMemberByUserName(@Param("username") String username);

    @Query("SELECT m FROM Member m")
    java.util.List<Member> findAllMembers();
}
