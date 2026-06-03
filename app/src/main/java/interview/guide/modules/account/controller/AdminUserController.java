package interview.guide.modules.account.controller;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.account.dto.AdminUserDTO;
import interview.guide.modules.account.dto.CreateUserRequest;
import interview.guide.modules.account.dto.ResetPasswordRequest;
import interview.guide.modules.account.dto.UpdateUserRequest;
import interview.guide.modules.account.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 30)
    public Result<List<AdminUserDTO>> listUsers(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Boolean enabled) {
        return Result.success(adminUserService.listUsers(keyword, role, enabled));
    }

    @GetMapping("/{id}")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 30)
    public Result<AdminUserDTO> getUser(@PathVariable Long id) {
        return Result.success(adminUserService.getUser(id));
    }

    @PostMapping
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    public Result<AdminUserDTO> createUser(@RequestBody @Valid CreateUserRequest request) {
        return Result.success(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    public Result<AdminUserDTO> updateUser(@PathVariable Long id,
                                           @RequestBody @Valid UpdateUserRequest request) {
        return Result.success(adminUserService.updateUser(id, request));
    }

    @PutMapping("/{id}/password")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody @Valid ResetPasswordRequest request) {
        adminUserService.resetPassword(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return Result.success();
    }
}
