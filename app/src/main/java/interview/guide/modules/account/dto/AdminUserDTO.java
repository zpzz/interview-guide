package interview.guide.modules.account.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminUserDTO(
    Long id,
    String username,
    String nickname,
    Boolean enabled,
    Set<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean protectedAccount
) {}
