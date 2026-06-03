package interview.guide.modules.account.repository;

import interview.guide.modules.account.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserEntity u "
        + "LEFT JOIN FETCH u.userRoles ur "
        + "LEFT JOIN FETCH ur.role "
        + "WHERE u.username = :username")
    Optional<UserEntity> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT u.id FROM UserEntity u "
        + "WHERE (LOWER(u.username) LIKE :keywordPattern "
        + "OR LOWER(COALESCE(u.nickname, '')) LIKE :keywordPattern) "
        + "AND (:enabled IS NULL OR u.enabled = :enabled) "
        + "AND (:role IS NULL OR EXISTS ("
        + "  SELECT 1 FROM UserRoleEntity ur2 WHERE ur2.user = u AND ur2.role.code = :role"
        + ")) "
        + "ORDER BY u.createdAt DESC")
    List<Long> searchIds(@Param("keywordPattern") String keywordPattern,
                         @Param("role") String role,
                         @Param("enabled") Boolean enabled);

    @Query("SELECT DISTINCT u FROM UserEntity u "
        + "LEFT JOIN FETCH u.userRoles ur "
        + "LEFT JOIN FETCH ur.role "
        + "WHERE u.id IN :ids "
        + "ORDER BY u.createdAt DESC")
    List<UserEntity> findAllByIdWithRoles(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT u FROM UserEntity u "
        + "LEFT JOIN FETCH u.userRoles ur "
        + "LEFT JOIN FETCH ur.role "
        + "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT COUNT(ur) > 0 FROM UserRoleEntity ur "
        + "WHERE ur.user.id = :userId AND ur.role.code = 'ADMIN'")
    boolean hasAdminRole(@Param("userId") Long userId);
}
