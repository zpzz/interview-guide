package interview.guide.modules.account.service;

import interview.guide.common.auth.CurrentUser;
import interview.guide.common.auth.JwtService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.account.dto.AuthResponse;
import interview.guide.modules.account.dto.LoginRequest;
import interview.guide.modules.account.dto.UserDTO;
import interview.guide.modules.account.model.UserEntity;
import interview.guide.modules.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameWithRoles(request.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_INVALID));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_INVALID);
        }

        Set<String> roles = user.getUserRoles().stream()
            .map(userRole -> userRole.getRole().getCode())
            .collect(Collectors.toUnmodifiableSet());
        CurrentUser currentUser = new CurrentUser(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            roles
        );
        return new AuthResponse(
            jwtService.generateToken(currentUser),
            new UserDTO(user.getId(), user.getUsername(), user.getNickname(), roles)
        );
    }
}
