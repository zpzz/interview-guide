package interview.guide.modules.resumemaker.dto;

public record GenerateResumePreviewResponse(
    Long generatedResumeId,
    Long sourceResumeId,
    String sourceResumeName,
    String title,
    String templateId,
    String htmlContent,
    ResumeGeneratedContentDTO generatedContent
) {
}
