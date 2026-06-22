package interview.guide.modules.resumemaker.repository;

import interview.guide.modules.resumemaker.model.ResumeTemplateAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeTemplateAssetRepository extends JpaRepository<ResumeTemplateAssetEntity, Long> {

    List<ResumeTemplateAssetEntity> findAllByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<ResumeTemplateAssetEntity> findByIdAndUserId(Long id, Long userId);
}
