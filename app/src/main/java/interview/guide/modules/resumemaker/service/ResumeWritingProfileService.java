package interview.guide.modules.resumemaker.service;

import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resumemaker.dto.ResumeProfileDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历编写业务 skill 的“输入理解”部分：
 * 从已上传简历文本中提取基础信息，生成更适合交给模型处理的 Profile。
 */
@Service
public class ResumeWritingProfileService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1\\d{10})(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern NAME_LINE_PATTERN = Pattern.compile("^(?:姓名[:：]\\s*)?([\\u4e00-\\u9fa5]{2,4})$");
    private static final Pattern TARGET_TITLE_PATTERN = Pattern.compile(
        "([\\u4e00-\\u9fa5A-Za-z]+(?:工程师|开发|前端|后端|全栈|架构师|产品经理|测试))"
    );

    private final ResumeWritingGuardService guardService;

    public ResumeWritingProfileService(ResumeWritingGuardService guardService) {
        this.guardService = guardService;
    }

    public ResumeProfileDTO buildProfileFromResume(ResumeEntity resume) {
        String rawText = resume.getResumeText() == null ? "" : resume.getResumeText();
        String normalizedText = rawText.replace("\r", "");
        ResumeProfileDTO.BasicInfoDTO basicInfo = new ResumeProfileDTO.BasicInfoDTO(
            extractName(normalizedText),
            extractByPattern(PHONE_PATTERN, normalizedText),
            extractByPattern(EMAIL_PATTERN, normalizedText),
            extractCity(normalizedText),
            extractTargetTitle(normalizedText),
            ""
        );
        return new ResumeProfileDTO(
            normalizedText,
            basicInfo,
            extractSummary(normalizedText),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null
        );
    }

    private String extractName(String text) {
        for (String line : text.split("\n")) {
            String cleaned = guardService.sanitizeIdentityField(line.replaceFirst("^姓名[:：]\\s*", "").trim());
            if (cleaned.isBlank()) {
                continue;
            }
            Matcher matcher = NAME_LINE_PATTERN.matcher(cleaned);
            if (matcher.matches()) {
                return guardService.sanitizeIdentityField(matcher.group(1));
            }
        }
        return "";
    }

    private String extractCity(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.contains("城市") && !trimmed.contains("现居") && !trimmed.contains("地点")) {
                continue;
            }
            String value = trimmed.replaceFirst("^.*?[城市现居地点][:：]\\s*", "").trim();
            value = guardService.sanitizeIdentityField(value);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String extractTargetTitle(String text) {
        Matcher matcher = TARGET_TITLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = guardService.sanitizeIdentityField(matcher.group(1));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String extractSummary(String text) {
        for (String block : text.split("\n\n")) {
            String sanitized = guardService.sanitizeSummaryLine(block.replace("\n", " ").trim());
            if (sanitized.length() >= 30) {
                return sanitized;
            }
        }
        return guardService.buildFallbackSummary("", text);
    }

    private String extractByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return guardService.sanitizeIdentityField(matcher.group());
        }
        return "";
    }
}
