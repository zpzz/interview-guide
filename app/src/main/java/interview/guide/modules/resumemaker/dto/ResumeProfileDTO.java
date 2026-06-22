package interview.guide.modules.resumemaker.dto;

import java.util.List;

public record ResumeProfileDTO(
    String rawText,
    BasicInfoDTO basicInfo,
    String summary,
    List<EducationDTO> educations,
    List<ExperienceDTO> experiences,
    List<ProjectDTO> projects,
    List<SkillCategoryDTO> skillCategories,
    List<String> awards,
    String additionalInfo
) {

    public record BasicInfoDTO(
        String name,
        String phone,
        String email,
        String city,
        String targetTitle,
        String personalSite
    ) {
    }

    public record EducationDTO(
        String school,
        String degree,
        String major,
        String startDate,
        String endDate,
        String description
    ) {
    }

    public record ExperienceDTO(
        String company,
        String title,
        String startDate,
        String endDate,
        String description,
        List<String> highlights
    ) {
    }

    public record ProjectDTO(
        String name,
        String role,
        String startDate,
        String endDate,
        String description,
        List<String> highlights,
        List<String> techStack
    ) {
    }

    public record SkillCategoryDTO(
        String category,
        List<String> items
    ) {
    }
}
