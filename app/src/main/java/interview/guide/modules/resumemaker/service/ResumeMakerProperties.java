package interview.guide.modules.resumemaker.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.resume-maker")
public class ResumeMakerProperties {

    private String systemPromptPath = "classpath:prompts/resume-maker/resume-maker-system.st";
    private String generatePromptPath = "classpath:prompts/resume-maker/resume-maker-generate-user.st";
    private String optimizePromptPath = "classpath:prompts/resume-maker/resume-maker-optimize-user.st";
    private String defaultTemplateId = "elegant-red";
}
