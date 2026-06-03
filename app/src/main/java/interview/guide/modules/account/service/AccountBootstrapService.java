package interview.guide.modules.account.service;

import interview.guide.modules.account.model.RoleEntity;
import interview.guide.modules.account.model.UserEntity;
import interview.guide.modules.account.model.UserRoleEntity;
import interview.guide.modules.account.repository.RoleRepository;
import interview.guide.modules.account.repository.UserRepository;
import interview.guide.modules.account.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBootstrapService implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RoleEntity userRole = roleRepository.findByCode("USER")
            .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                .code("USER")
                .name("普通用户")
                .description("只能访问自己创建的数据")
                .build()));
        RoleEntity adminRole = roleRepository.findByCode("ADMIN")
            .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                .code("ADMIN")
                .name("管理员")
                .description("可以访问所有用户数据")
                .build()));

        createUserIfAbsent("user", "user123", "普通用户", userRole);
        createUserIfAbsent("admin", "admin123", "管理员", adminRole);
    }

    private void createUserIfAbsent(String username, String rawPassword, String nickname, RoleEntity role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        UserEntity user = userRepository.save(UserEntity.builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword))
            .nickname(nickname)
            .enabled(true)
            .build());
        userRoleRepository.save(UserRoleEntity.builder()
            .user(user)
            .role(role)
            .build());
        log.info("Initialized default account: username={}, role={}", username, role.getCode());
    }
}
