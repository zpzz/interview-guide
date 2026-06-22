package interview.guide.modules.resumemaker.dto;

public record ResumeTemplateDTO(
    String id,
    String name,
    String description,
    String accentColor,
    String layoutType,
    String recommendedScenario,
    String previewImageUrl,
    String referenceHtmlUrl,
    String referencePdfUrl
) {
}
