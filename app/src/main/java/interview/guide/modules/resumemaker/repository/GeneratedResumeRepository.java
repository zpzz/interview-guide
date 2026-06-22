package interview.guide.modules.resumemaker.repository;

import interview.guide.modules.resumemaker.model.GeneratedResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedResumeRepository extends JpaRepository<GeneratedResumeEntity, Long> {

    List<GeneratedResumeEntity> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<GeneratedResumeEntity> findByIdAndUserId(Long id, Long userId);
}
