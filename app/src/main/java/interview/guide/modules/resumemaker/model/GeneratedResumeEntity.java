package interview.guide.modules.resumemaker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_resumes", indexes = {
    @Index(name = "idx_generated_resume_user_updated", columnList = "user_id,updated_at")
})
public class GeneratedResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_resume_id", nullable = false)
    private Long sourceResumeId;

    @Column(name = "source_resume_name", nullable = false, length = 255)
    private String sourceResumeName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "target_role", length = 255)
    private String targetRole;

    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "builtin_template_id", length = 64)
    private String builtinTemplateId;

    @Column(name = "template_asset_id")
    private Long templateAssetId;

    @Column(name = "provider_id", length = 64)
    private String providerId;

    @Column(name = "target_page_count")
    private Integer targetPageCount;

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
