package interview.guide.modules.account.service;

import interview.guide.common.auth.CurrentUser;
import interview.guide.common.auth.CurrentUserService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.account.dto.AdminUserDTO;
import interview.guide.modules.account.dto.CreateUserRequest;
import interview.guide.modules.account.dto.ResetPasswordRequest;
import interview.guide.modules.account.dto.UpdateUserRequest;
import interview.guide.modules.account.model.RoleEntity;
import interview.guide.modules.account.model.UserEntity;
import interview.guide.modules.account.model.UserRoleEntity;
import interview.guide.modules.account.repository.RoleRepository;
import interview.guide.modules.account.repository.UserRepository;
import interview.guide.modules.account.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String PROTECTED_ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<AdminUserDTO> listUsers(String keyword, String role, Boolean enabled) {
        requireAdmin();
        String normalizedKeyword = normalize(keyword);
        String keywordPattern = normalizedKeyword == null
            ? "%"
            : "%" + normalizedKeyword.toLowerCase() + "%";
        String normalizedRole = normalizeRole(role);
        List<Long> userIds = userRepository.searchIds(keywordPattern, normalizedRole, enabled);
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByIdWithRoles(userIds).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDTO getUser(Long id) {
        requireAdmin();
        return toDTO(getUserOrThrow(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserDTO createUser(CreateUserRequest request) {
        requireAdmin();
        String username = requireNonBlank(request.username(), "账号不能为空");
        if (isProtectedAdmin(username)) {
            throw new BusinessException(ErrorCode.PROTECTED_USER_CANNOT_MODIFY, "不能新增默认 admin 账号");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        String password = requireNonBlank(request.password(), "密码不能为空");
        Set<RoleEntity> roles = resolveRoles(request.roles());

        UserEntity user = userRepository.save(UserEntity.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .nickname(normalize(request.nickname()))
            .enabled(request.enabled() == null ? true : request.enabled())
            .build());
        saveRoles(user, roles);
        return toDTO(getUserOrThrow(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserDTO updateUser(Long id, UpdateUserRequest request) {
        CurrentUser currentUser = requireAdmin();
        UserEntity user = getUserOrThrow(id);
        ensureNotProtectedAdmin(user);
        Set<RoleEntity> roles = resolveRoles(request.roles());
        boolean removingSelfAdmin = currentUser.id().equals(id)
            && roles.stream().noneMatch(role -> ADMIN_ROLE.equals(role.getCode()));
        if (removingSelfAdmin) {
            throw new BusinessException(ErrorCode.CURRENT_USER_ADMIN_ROLE_REQUIRED);
        }
        if (currentUser.id().equals(id) && Boolean.FALSE.equals(request.enabled())) {
            throw new BusinessException(ErrorCode.CURRENT_USER_ADMIN_ROLE_REQUIRED, "不能禁用当前登录管理员");
        }

        user.setNickname(normalize(request.nickname()));
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        UserEntity saved = userRepository.save(user);
        userRoleRepository.deleteByUserId(saved.getId());
        saveRoles(saved, roles);
        return toDTO(getUserOrThrow(saved.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, ResetPasswordRequest request) {
        requireAdmin();
        UserEntity user = getUserOrThrow(id);
        ensureNotProtectedAdmin(user);
        String password = requireNonBlank(request.password(), "密码不能为空");
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        CurrentUser currentUser = requireAdmin();
        if (currentUser.id().equals(id)) {
            throw new BusinessException(ErrorCode.CURRENT_USER_CANNOT_DELETE);
        }
        UserEntity user = getUserOrThrow(id);
        ensureNotProtectedAdmin(user);
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    private CurrentUser requireAdmin() {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
        return currentUser;
    }

    private UserEntity getUserOrThrow(Long id) {
        return userRepository.findByIdWithRoles(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个角色");
        }
        return roleCodes.stream()
            .map(this::normalizeRole)
            .map(roleCode -> roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在: " + roleCode)))
            .collect(Collectors.toSet());
    }

    private void saveRoles(UserEntity user, Set<RoleEntity> roles) {
        List<UserRoleEntity> userRoles = roles.stream()
            .map(role -> UserRoleEntity.builder()
                .user(user)
                .role(role)
                .build())
            .toList();
        userRoleRepository.saveAll(userRoles);
    }

    private AdminUserDTO toDTO(UserEntity user) {
        Set<String> roles = user.getUserRoles().stream()
            .map(userRole -> userRole.getRole().getCode())
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new AdminUserDTO(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEnabled(),
            roles,
            user.getCreatedAt(),
            user.getUpdatedAt(),
            isProtectedAdmin(user.getUsername())
        );
    }

    private void ensureNotProtectedAdmin(UserEntity user) {
        if (isProtectedAdmin(user.getUsername())) {
            throw new BusinessException(ErrorCode.PROTECTED_USER_CANNOT_MODIFY);
        }
    }

    private boolean isProtectedAdmin(String username) {
        return PROTECTED_ADMIN_USERNAME.equalsIgnoreCase(username);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRole(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String requireNonBlank(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return normalized;
    }
}
