package interview.guide.modules.resumemaker.dto;

import java.time.LocalDateTime;

public record GeneratedResumeListItemDTO(
    Long id,
    String title,
    Long sourceResumeId,
    String sourceResumeName,
    String targetRole,
    String builtinTemplateId,
    Long templateAssetId,
    Integer targetPageCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
