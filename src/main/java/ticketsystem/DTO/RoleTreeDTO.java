package ticketsystem.DTO;

import java.util.List;

public record RoleTreeDTO(
        Long memberId,
        String memberName,
        String roleType,
        Long appointedByMemberId,
        String appointedByName,
        List<String> permissions,
        List<RoleTreeDTO> children
) {
}