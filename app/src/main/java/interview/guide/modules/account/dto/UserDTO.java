package interview.guide.modules.account.dto;

import java.util.Set;

public record UserDTO(
    Long id,
    String username,
    String nickname,
    Set<String> roles
) {}
