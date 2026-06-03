package interview.guide.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CreateUserRequest(
    @NotBlank(message = "账号不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password,

    String nickname,

    Boolean enabled,

    @NotEmpty(message = "至少选择一个角色")
    Set<String> roles
) {}
