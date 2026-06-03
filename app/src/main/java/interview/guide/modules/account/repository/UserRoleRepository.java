package interview.guide.modules.account.repository;

import interview.guide.modules.account.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    void deleteByUserId(Long userId);
}
