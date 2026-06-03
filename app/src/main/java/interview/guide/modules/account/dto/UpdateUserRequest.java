package interview.guide.modules.account.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRequest(
    String nickname,
    Boolean enabled,

    @NotEmpty(message = "至少选择一个角色")
    Set<String> roles
) {}
