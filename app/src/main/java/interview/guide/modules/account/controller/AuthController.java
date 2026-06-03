package interview.guide.modules.account.controller;

import interview.guide.common.result.Result;
import interview.guide.modules.account.dto.AuthResponse;
import interview.guide.modules.account.dto.LoginRequest;
import interview.guide.modules.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/login")
    public Result<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
