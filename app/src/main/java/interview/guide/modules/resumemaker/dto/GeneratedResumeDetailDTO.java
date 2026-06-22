package interview.guide.modules.resumemaker.dto;

import java.time.LocalDateTime;

public record GeneratedResumeDetailDTO(
    Long id,
    Long sourceResumeId,
    String sourceResumeName,
    String title,
    String targetRole,
    String jdText,
    String builtinTemplateId,
    Long templateAssetId,
    String providerId,
    Integer targetPageCount,
    String htmlContent,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
