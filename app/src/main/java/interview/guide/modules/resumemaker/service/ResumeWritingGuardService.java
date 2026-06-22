package interview.guide.modules.resumemaker.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 简历编写规则守卫：
 * 负责清洗占位词、兜底标题、摘要文本裁剪，避免脏数据直接进入模型或模板。
 */
@Service
public class ResumeWritingGuardService {

    private static final List<String> PLACEHOLDER_TOKENS = List.of(
        "待补充", "候选人", "未知", "暂无", "未提供", "未填写"
    );

    public String sanitizeIdentityField(String value) {
        if (isBlank(value)) {
            return "";
        }
        String sanitized = value.trim();
        for (String token : PLACEHOLDER_TOKENS) {
            if (sanitized.contains(token)) {
                sanitized = sanitized.replace(token, "").trim();
            }
        }
        return "null".equalsIgnoreCase(sanitized) ? "" : sanitized;
    }

    public String sanitizeSummaryLine(String value) {
        return sanitizeIdentityField(value).replaceAll("\\s+", " ").trim();
    }

    public String buildHeadline(String modelHeadline, String profileTitle, String targetRole) {
        return firstNonBlank(
            sanitizeIdentityField(modelHeadline),
            sanitizeIdentityField(profileTitle),
            sanitizeIdentityField(targetRole),
            "简历优化版"
        );
    }

    public String buildFallbackSummary(String summary, String rawText) {
        if (notBlank(summary)) {
            return sanitizeSummaryLine(summary);
        }
        if (notBlank(rawText)) {
            String normalized = sanitizeSummaryLine(rawText.replace("\r", "").trim());
            if (!notBlank(normalized)) {
                return "";
            }
            return normalized.length() <= 180 ? normalized : normalized.substring(0, 180).trim() + "...";
        }
        return "";
    }

    public boolean notBlank(String value) {
        return !isBlank(value);
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }
}
