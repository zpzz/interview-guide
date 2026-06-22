package interview.guide.modules.resumemaker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.resumemaker.dto.ResumeProfileDTO;
import lombok.Getter;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 简历编写业务 skill 的“提示词构建”部分：
 * 把规则化输入转换为模型实际调用的 system/user prompt。
 */
@Service
public class ResumeWritingPromptService {

    private final ObjectMapper objectMapper;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate generatePromptTemplate;
    private final PromptTemplate optimizePromptTemplate;

    public ResumeWritingPromptService(ObjectMapper objectMapper,
                                      ResumeMakerProperties properties,
                                      ResourceLoader resourceLoader) throws IOException {
        this.objectMapper = objectMapper;
        this.systemPromptTemplate = new PromptTemplate(
            resourceLoader.getResource(properties.getSystemPromptPath())
                .getContentAsString(StandardCharsets.UTF_8)
        );
        this.generatePromptTemplate = new PromptTemplate(
            resourceLoader.getResource(properties.getGeneratePromptPath())
                .getContentAsString(StandardCharsets.UTF_8)
        );
        this.optimizePromptTemplate = new PromptTemplate(
            resourceLoader.getResource(properties.getOptimizePromptPath())
                .getContentAsString(StandardCharsets.UTF_8)
        );
    }

    public BuiltPrompt buildPrompt(ResumeProfileDTO profile,
                                   String targetRole,
                                   String jdText,
                                   String formatInstruction,
                                   boolean optimize) {
        try {
            String profileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            Map<String, Object> variables = new HashMap<>();
            variables.put("targetRole", blankToDefault(targetRole, "未指定岗位"));
            variables.put("jdText", blankToDefault(jdText, "未提供 JD"));
            variables.put("profileJson", profileJson);
            return new BuiltPrompt(
                systemPromptTemplate.render() + "\n\n" + formatInstruction,
                (optimize ? optimizePromptTemplate : generatePromptTemplate).render(variables)
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "构建简历生成提示词失败");
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Getter
    public static class BuiltPrompt {
        private final String systemPrompt;
        private final String userPrompt;

        public BuiltPrompt(String systemPrompt, String userPrompt) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
        }
    }
}
