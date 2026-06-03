package interview.guide.modules.account.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "密码不能为空")
    String password
) {}
