package interview.guide.modules.llmprovider.repository;

import interview.guide.modules.llmprovider.model.LlmUserSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmUserSettingRepository extends JpaRepository<LlmUserSettingEntity, Long> {
}
