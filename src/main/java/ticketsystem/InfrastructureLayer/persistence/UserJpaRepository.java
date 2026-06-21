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

    @Query("SELECT m FROM Member m WHERE LOWER(m.userName) = LOWER(:username)")
    Optional<Member> findMemberByUserNameIgnoreCase(@Param("username") String username);

    @Query("""
            SELECT DISTINCT m FROM Member m JOIN m.companyRoles r
            WHERE r.companyId = :companyId
            """)
    java.util.List<Member> findMembersWithRolesInCompany(@Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Member m
            WHERE m.suspension IS NOT NULL
              AND m.suspension.revoked = false
              AND (m.suspension.endDate IS NULL OR m.suspension.endDate > CURRENT_TIMESTAMP)
            """)
    java.util.List<Member> findSuspendedMembers();

    @Query("SELECT COUNT(r) FROM CompanyRole r WHERE r.companyId = :companyId AND r.status = 'PENDING'")
    int countPendingRolesByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(r) FROM Owner r WHERE r.companyId = :companyId AND r.status = 'ACTIVE'")
    int countActiveOwnersByCompanyId(@Param("companyId") Long companyId);
}
