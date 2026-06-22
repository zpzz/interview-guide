package interview.guide.modules.resumemaker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateResumePreviewRequest(
    @NotNull Long sourceResumeId,
    @NotBlank String sourceResumeName,
    String title,
    String targetRole,
    String jdText,
    String extraNotes,
    @Min(1) @Max(10) Integer targetPageCount,
    String builtinTemplateId,
    Long templateAssetId,
    String providerId
) {
}
