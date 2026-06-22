package interview.guide.modules.resumemaker.dto;

import java.time.LocalDateTime;

public record ResumeTemplateAssetDTO(
    Long id,
    String originalFilename,
    String contentType,
    Long fileSize,
    String storageUrl,
    LocalDateTime uploadedAt
) {
}
