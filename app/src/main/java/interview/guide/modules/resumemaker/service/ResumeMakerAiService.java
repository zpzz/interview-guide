package interview.guide.modules.resumemaker.service;

import interview.guide.common.ai.LlmProviderRegistry;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.resumemaker.dto.ResumeGeneratedContentDTO;
import interview.guide.modules.resumemaker.dto.ResumeProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class ResumeMakerAiService {

    private final LlmProviderRegistry llmProviderRegistry;
    private final ResumeWritingPromptService promptService;
    private final ResumeWritingGuardService guardService;
    private final BeanOutputConverter<ResumeGeneratedContentDTO> outputConverter;
    private final String htmlSystemPrompt;
    private final String htmlUserPromptTemplate;

    public ResumeMakerAiService(
        LlmProviderRegistry llmProviderRegistry,
        ResumeWritingPromptService promptService,
        ResumeWritingGuardService guardService,
        ResourceLoader resourceLoader
    ) throws IOException {
        this.llmProviderRegistry = llmProviderRegistry;
        this.promptService = promptService;
        this.guardService = guardService;
        this.outputConverter = new BeanOutputConverter<>(ResumeGeneratedContentDTO.class);
        this.htmlSystemPrompt = resourceLoader
            .getResource("classpath:prompts/resume-maker/resume-maker-html-system.st")
            .getContentAsString(StandardCharsets.UTF_8);
        this.htmlUserPromptTemplate = resourceLoader
            .getResource("classpath:prompts/resume-maker/resume-maker-html-user.st")
            .getContentAsString(StandardCharsets.UTF_8);
    }

    public ResumeGeneratedContentDTO generate(ResumeProfileDTO profile,
                                              String targetRole,
                                              String jdText,
                                              Long userId,
                                              String providerId) {
        return invoke(profile, targetRole, jdText, userId, providerId, false);
    }

    public ResumeGeneratedContentDTO optimize(ResumeProfileDTO profile,
                                              String targetRole,
                                              String jdText,
                                              Long userId,
                                              String providerId) {
        return invoke(profile, targetRole, jdText, userId, providerId, true);
    }

    public String generateHtml(ResumeProfileDTO profile,
                               String title,
                               String targetRole,
                               String jdText,
                               String extraNotes,
                               Integer targetPageCount,
                               String templateReferenceHtml,
                               Long userId,
                               String providerId) {
        validateProfile(profile);
        String resolvedProviderId = llmProviderRegistry.resolvePlainChatProviderIdForUser(userId, providerId);
        try {
            ChatClient chatClient = llmProviderRegistry.getPlainChatClient(resolvedProviderId);
            String userPrompt = renderHtmlUserPrompt(
                blankToDefault(title, "简历优化版"),
                blankToDefault(targetRole, "未指定岗位"),
                trimPromptText(jdText, 2500, "未提供 JD"),
                trimPromptText(extraNotes, 800, "未提供其他描述"),
                targetPageCount,
                trimResumeText(profile.rawText()),
                blankToDefault(templateReferenceHtml, "")
            );
            String html = chatClient.prompt()
                .system(htmlSystemPrompt)
                .user(userPrompt)
                .call()
                .content();
            logGeneratedHtmlResponse(userId, resolvedProviderId, title, targetRole, html);
            String normalized = normalizeHtml(html);
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                "AI 简历 HTML 生成失败: targetRole={}, {}",
                targetRole,
                safeDescribeProvider(resolvedProviderId),
                e
            );
            throw new BusinessException(
                ErrorCode.RESUME_GENERATION_FAILED,
                buildGenerateHtmlErrorMessage(e, resolvedProviderId)
            );
        }
    }

    /**
     * 这里统一走结构化 JSON 输出，让模型只负责内容组织，页面结构交给模板层控制。
     */
    private ResumeGeneratedContentDTO invoke(ResumeProfileDTO profile,
                                             String targetRole,
                                             String jdText,
                                             Long userId,
                                             String providerId,
                                             boolean optimize) {
        validateProfile(profile);
        try {
            ResumeWritingPromptService.BuiltPrompt builtPrompt = promptService.buildPrompt(
                profile, targetRole, jdText, outputConverter.getFormat(), optimize
            );
            ChatClient chatClient = providerId != null && !providerId.isBlank()
                ? llmProviderRegistry.getPlainChatClient(providerId)
                : llmProviderRegistry.getDefaultPlainChatClientForUser(userId);
            ResumeGeneratedContentDTO content = outputConverter.convert(
                chatClient.prompt()
                    .system(builtPrompt.getSystemPrompt())
                    .user(builtPrompt.getUserPrompt())
                    .call()
                    .content()
            );
            ResumeGeneratedContentDTO normalizedContent = normalizeGeneratedContent(content, profile, targetRole);
            validateGeneratedContent(normalizedContent);
            return normalizedContent;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 简历生成失败: optimize={}, targetRole={}", optimize, targetRole, e);
            throw new BusinessException(ErrorCode.RESUME_GENERATION_FAILED, "AI 简历生成失败：" + e.getMessage());
        }
    }

    private void validateProfile(ResumeProfileDTO profile) {
        if (profile == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先填写个人信息");
        }
        boolean hasRawText = guardService.notBlank(profile.rawText());
        boolean hasBasicInfo = profile.basicInfo() != null && (
            guardService.notBlank(profile.basicInfo().name())
                || guardService.notBlank(profile.basicInfo().phone())
                || guardService.notBlank(profile.basicInfo().email())
                || guardService.notBlank(profile.basicInfo().city())
                || guardService.notBlank(profile.basicInfo().targetTitle())
                || guardService.notBlank(profile.basicInfo().personalSite())
        );
        if (!hasRawText && !hasBasicInfo) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少填写个人信息原文或基础信息");
        }
    }

    private void validateGeneratedContent(ResumeGeneratedContentDTO content) {
        if (content == null || content.basicInfo() == null) {
            throw new BusinessException(ErrorCode.RESUME_GENERATION_FAILED, "模型未返回有效的简历内容");
        }
    }

    /**
     * 模型擅长润色模块内容，但姓名、电话、邮箱、城市这类身份字段必须以用户输入为准，
     * 否则模型很容易回退到“张三/13800138000”这类训练语料里的示例值。
     */
    private ResumeGeneratedContentDTO normalizeGeneratedContent(ResumeGeneratedContentDTO content,
                                                                ResumeProfileDTO profile,
                                                                String targetRole) {
        ResumeProfileDTO.BasicInfoDTO inputBasic = profile.basicInfo() != null
            ? profile.basicInfo()
            : new ResumeProfileDTO.BasicInfoDTO("", "", "", "", "", "");
        ResumeProfileDTO.BasicInfoDTO modelBasic = content.basicInfo() != null
            ? content.basicInfo()
            : new ResumeProfileDTO.BasicInfoDTO("", "", "", "", "", "");
        ResumeProfileDTO.BasicInfoDTO mergedBasic = new ResumeProfileDTO.BasicInfoDTO(
            guardService.sanitizeIdentityField(preferInput(inputBasic.name(), modelBasic.name())),
            guardService.sanitizeIdentityField(preferInput(inputBasic.phone(), modelBasic.phone())),
            guardService.sanitizeIdentityField(preferInput(inputBasic.email(), modelBasic.email())),
            guardService.sanitizeIdentityField(preferInput(inputBasic.city(), modelBasic.city())),
            guardService.sanitizeIdentityField(preferInput(inputBasic.targetTitle(), modelBasic.targetTitle())),
            guardService.sanitizeIdentityField(preferInput(inputBasic.personalSite(), modelBasic.personalSite()))
        );

        String mergedHeadline = guardService.buildHeadline(
            content.headline(), inputBasic.targetTitle(), targetRole
        );

        List<String> mergedSummaryHighlights = content.summaryHighlights() == null || content.summaryHighlights().isEmpty()
            ? buildFallbackSummary(profile)
            : content.summaryHighlights();

        return new ResumeGeneratedContentDTO(
            mergedBasic,
            mergedHeadline,
            mergedSummaryHighlights,
            content.educations(),
            content.experiences(),
            content.projects(),
            content.skillCategories(),
            content.awards(),
            content.extraSections()
        );
    }

    private List<String> buildFallbackSummary(ResumeProfileDTO profile) {
        String fallback = guardService.buildFallbackSummary(profile.summary(), profile.rawText());
        return guardService.notBlank(fallback) ? List.of(fallback) : List.of();
    }

    private String preferInput(String inputValue, String modelValue) {
        return guardService.notBlank(inputValue) ? inputValue : (modelValue == null ? "" : modelValue);
    }

    private boolean notBlank(String value) {
        return guardService.notBlank(value);
    }

    private String blankToDefault(String value, String defaultValue) {
        return guardService.notBlank(value) ? value : defaultValue;
    }

    private String normalizeHtml(String html) {
        if (html == null) {
            return "";
        }
        String normalized = html.trim();
        if (normalized.startsWith("```html")) {
            normalized = normalized.substring(7).trim();
        }
        if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }
        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }
        return normalized;
    }

    private String renderHtmlUserPrompt(String title,
                                        String targetRole,
                                        String jdText,
                                        String extraNotes,
                                        Integer targetPageCount,
                                        String resumeText,
                                        String templateReferenceHtml) {
        return htmlUserPromptTemplate
            .replace("<title>", title)
            .replace("<targetRole>", targetRole)
            .replace("<jdText>", jdText)
            .replace("<extraNotes>", extraNotes)
            .replace("<targetPageCount>", targetPageCount == null ? "1" : String.valueOf(targetPageCount))
            .replace("<resumeText>", resumeText)
            .replace("<templateReferenceHtml>", templateReferenceHtml);
    }

    private String buildGenerateHtmlErrorMessage(Exception e, String providerId) {
        String message = e.getMessage();
        String providerDescription = safeDescribeProvider(providerId);
        if (message != null && message.contains("application/octet-stream")) {
            return "AI 简历生成失败：当前模型服务返回了非 OpenAI 兼容的响应格式，请检查 Provider 配置，"
                + providerDescription;
        }
        if (message != null && !message.isBlank()) {
            return "AI 简历生成失败：" + message + "（" + providerDescription + "）";
        }
        return "AI 简历生成失败，请稍后重试（" + providerDescription + "）";
    }

    private String safeDescribeProvider(String providerId) {
        try {
            return llmProviderRegistry.describeProvider(providerId);
        } catch (Exception ex) {
            log.warn("读取 Provider 描述失败: providerId={}", providerId, ex);
            return "provider=" + providerId;
        }
    }

    private String trimResumeText(String resumeText) {
        if (!guardService.notBlank(resumeText)) {
            return "";
        }
        String normalized = resumeText
            .replace('\u0000', ' ')
            .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
        return normalized.length() <= 8000 ? normalized : normalized.substring(0, 8000);
    }

    private String trimPromptText(String text, int maxLength, String defaultValue) {
        if (!guardService.notBlank(text)) {
            return defaultValue;
        }
        String normalized = text
            .replace('\u0000', ' ')
            .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private void logGeneratedHtmlResponse(Long userId,
                                          String providerId,
                                          String title,
                                          String targetRole,
                                          String html) {
        if (!log.isInfoEnabled()) {
            return;
        }
        log.info(
            "简历生成模型响应: userId={}, providerId={}, title={}, targetRole={}, htmlLength={}",
            userId,
            providerId,
            summarize(title, 80),
            summarize(targetRole, 80),
            safeLength(html)
        );
        log.info("简历生成模型响应内容（截断）:\n{}", summarizeMultiline(html, 4000));
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String summarize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength
            ? normalized
            : normalized.substring(0, maxLength) + "...";
    }

    private String summarizeMultiline(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= maxLength
            ? value
            : value.substring(0, maxLength) + "\n...<truncated>";
    }
}
