package ticketsystem.DTO;

import ticketsystem.DomainLayer.user.*;
import java.util.List;
import java.util.stream.Collectors;

public class MemberDTO {
    
    private Long memberId;
    private String userName;
    private String fullName;
    private String phone;
    private boolean isSuspended;
    private List<CompanyRoleDTO> roles;

    public MemberDTO() {}

    public MemberDTO(Long memberId, String userName, String fullName, String phone, boolean isSuspended, List<CompanyRoleDTO> roles) {
        this.memberId = memberId;
        this.userName = userName;
        this.fullName = fullName;
        this.phone = phone;
        this.isSuspended = isSuspended;
        this.roles = roles;
    }

    public static MemberDTO fromDomain(Member member) {
        if (member == null) return null;

        List<CompanyRoleDTO> roleDTOs = member.getAllRoles().stream()
                .filter(role -> role.getStatus() != RoleStatus.CANCELLED)
                .map(CompanyRoleDTO::fromDomain)
                .collect(Collectors.toList());

        return new MemberDTO(
                member.getId(),
                member.getUserName(),
                member.getFullName(),
                member.getPhone(),
                member.isSuspended(),
                roleDTOs
        );
    }

    public Long getMemberId() { return memberId; }
    public String getUserName() { return userName; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public boolean isSuspended() { return isSuspended; }
    public List<CompanyRoleDTO> getRoles() { return roles; }

}