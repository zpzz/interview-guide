package interview.guide.modules.resumemaker.dto;

import java.util.List;

public record ResumeGeneratedContentDTO(
    ResumeProfileDTO.BasicInfoDTO basicInfo,
    String headline,
    List<String> summaryHighlights,
    List<GeneratedEducationDTO> educations,
    List<GeneratedExperienceDTO> experiences,
    List<GeneratedProjectDTO> projects,
    List<ResumeProfileDTO.SkillCategoryDTO> skillCategories,
    List<String> awards,
    List<ExtraSectionDTO> extraSections
) {

    public record GeneratedEducationDTO(
        String school,
        String degree,
        String major,
        String startDate,
        String endDate,
        List<String> highlights
    ) {
    }

    public record GeneratedExperienceDTO(
        String company,
        String title,
        String startDate,
        String endDate,
        List<String> highlights
    ) {
    }

    public record GeneratedProjectDTO(
        String name,
        String role,
        String startDate,
        String endDate,
        List<String> techStack,
        List<String> highlights
    ) {
    }

    public record ExtraSectionDTO(
        String title,
        List<String> items
    ) {
    }
}
