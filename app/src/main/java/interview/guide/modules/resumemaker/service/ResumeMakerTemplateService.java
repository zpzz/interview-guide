package interview.guide.modules.resumemaker.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.resumemaker.dto.ResumeGeneratedContentDTO;
import interview.guide.modules.resumemaker.dto.ResumeProfileDTO;
import interview.guide.modules.resumemaker.dto.ResumeTemplateDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Service
public class ResumeMakerTemplateService {

    private final ResourceLoader resourceLoader;
    private final Map<String, ResumeTemplateDTO> templates;

    public ResumeMakerTemplateService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.templates = loadTemplates(resourceLoader, objectMapper);
    }

    public List<ResumeTemplateDTO> listTemplates() {
        return List.copyOf(templates.values());
    }

    public ResumeTemplateDTO getTemplate(String templateId) {
        ResumeTemplateDTO template = templates.get(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.RESUME_TEMPLATE_NOT_FOUND, "简历模板不存在");
        }
        return template;
    }

    public String getReferenceHtml(String templateId) {
        ResumeTemplateDTO template = getTemplate(templateId);
        String referenceHtmlUrl = template.referenceHtmlUrl();
        if (referenceHtmlUrl == null || referenceHtmlUrl.isBlank()) {
            throw new BusinessException(ErrorCode.RESUME_TEMPLATE_NOT_FOUND, "内置简历模板 HTML 不存在");
        }
        String classpathPath = "classpath:static" + referenceHtmlUrl;
        try {
            return resourceLoader.getResource(classpathPath).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "读取内置简历模板 HTML 失败");
        }
    }

    /**
     * 模板渲染只接收结构化内容，避免直接信任模型输出的任意 HTML。
     */
    public String renderHtml(String templateId,
                             ResumeProfileDTO profile,
                             ResumeGeneratedContentDTO generatedContent) {
        ResumeTemplateDTO template = getTemplate(templateId);
        ResumeGeneratedContentDTO content = generatedContent != null
            ? generatedContent
            : fallbackContent(profile);
        return switch (templateId) {
            case "calm-dual" -> renderDualColumn(template, content);
            case "geek-chic" -> renderGeek(template, content);
            case "fresh-blue-gray" -> renderFreshBlueGray(template, content);
            case "minimal-white" -> renderMinimal(template, content);
            default -> renderElegant(template, content);
        };
    }

    private ResumeGeneratedContentDTO fallbackContent(ResumeProfileDTO profile) {
        ResumeProfileDTO.BasicInfoDTO basicInfo = profile != null && profile.basicInfo() != null
            ? profile.basicInfo()
            : new ResumeProfileDTO.BasicInfoDTO("", "", "", "", "", "");
        List<String> summaryHighlights = List.of();
        if (profile != null) {
            if (notBlank(profile.summary())) {
                summaryHighlights = List.of(profile.summary());
            } else if (notBlank(profile.rawText())) {
                summaryHighlights = List.of(profile.rawText());
            }
        }
        return new ResumeGeneratedContentDTO(
            basicInfo,
            safeText(basicInfo.targetTitle()),
            summaryHighlights,
            mapEducations(profile),
            mapExperiences(profile),
            mapProjects(profile),
            profile != null && profile.skillCategories() != null ? profile.skillCategories() : List.of(),
            profile != null && profile.awards() != null ? profile.awards() : List.of(),
            List.of()
        );
    }


    private List<ResumeGeneratedContentDTO.GeneratedEducationDTO> mapEducations(ResumeProfileDTO profile) {
        if (profile == null || profile.educations() == null) {
            return List.of();
        }
        return profile.educations().stream()
            .map(education -> new ResumeGeneratedContentDTO.GeneratedEducationDTO(
                education.school(),
                education.degree(),
                education.major(),
                education.startDate(),
                education.endDate(),
                notBlank(education.description()) ? List.of(education.description()) : List.of()
            ))
            .toList();
    }

    private List<ResumeGeneratedContentDTO.GeneratedExperienceDTO> mapExperiences(ResumeProfileDTO profile) {
        if (profile == null || profile.experiences() == null) {
            return List.of();
        }
        return profile.experiences().stream()
            .map(experience -> new ResumeGeneratedContentDTO.GeneratedExperienceDTO(
                experience.company(),
                experience.title(),
                experience.startDate(),
                experience.endDate(),
                mergeHighlights(experience.description(), experience.highlights())
            ))
            .toList();
    }

    private List<ResumeGeneratedContentDTO.GeneratedProjectDTO> mapProjects(ResumeProfileDTO profile) {
        if (profile == null || profile.projects() == null) {
            return List.of();
        }
        return profile.projects().stream()
            .map(project -> new ResumeGeneratedContentDTO.GeneratedProjectDTO(
                project.name(),
                project.role(),
                project.startDate(),
                project.endDate(),
                project.techStack() != null ? project.techStack() : List.of(),
                mergeHighlights(project.description(), project.highlights())
            ))
            .toList();
    }

    private List<String> mergeHighlights(String description, List<String> highlights) {
        String base = safeText(description);
        if (highlights == null || highlights.isEmpty()) {
            return notBlank(base) ? List.of(base) : List.of();
        }
        if (!notBlank(base)) {
            return highlights;
        }
        return new java.util.ArrayList<>() {{
            add(base);
            addAll(highlights);
        }};
    }

    private String renderElegant(ResumeTemplateDTO template, ResumeGeneratedContentDTO content) {
        return baseHtml(template, """
            <div class="resume single-column">
              %s
              %s
              %s
              %s
              %s
              %s
            </div>
            """.formatted(
            renderHeader(content),
            renderSummary(content),
            renderEducation(content),
            renderExperience(content),
            renderProjects(content),
            renderSkillColumns(content, false)
        ));
    }

    private String renderMinimal(ResumeTemplateDTO template, ResumeGeneratedContentDTO content) {
        return baseHtml(template, """
            <div class="resume single-column minimal">
              %s
              %s
              %s
              %s
              %s
              %s
            </div>
            """.formatted(
            renderHeader(content),
            renderSummary(content),
            renderExperience(content),
            renderProjects(content),
            renderEducation(content),
            renderSkillColumns(content, false)
        ));
    }

    private String renderGeek(ResumeTemplateDTO template, ResumeGeneratedContentDTO content) {
        return baseHtml(template, """
            <div class="resume single-column geek">
              %s
              %s
              %s
              %s
              %s
              %s
            </div>
            """.formatted(
            renderHeader(content),
            renderSummary(content),
            renderProjects(content),
            renderExperience(content),
            renderSkillColumns(content, false),
            renderEducation(content)
        ));
    }

    private String renderFreshBlueGray(ResumeTemplateDTO template, ResumeGeneratedContentDTO content) {
        return baseHtml(template, """
            <div class="resume single-column fresh-blue-gray">
              %s
              %s
              %s
              %s
              %s
              %s
            </div>
            """.formatted(
            renderHeader(content),
            renderSummary(content),
            renderEducation(content),
            renderExperience(content),
            renderProjects(content),
            renderSkillColumns(content, false)
        ));
    }

    private String renderDualColumn(ResumeTemplateDTO template, ResumeGeneratedContentDTO content) {
        return baseHtml(template, """
            <div class="resume dual-column">
              <div class="main-column">
                %s
                %s
                %s
                %s
              </div>
              <div class="side-column">
                %s
                %s
              </div>
            </div>
            """.formatted(
            renderHeader(content),
            renderSummary(content),
            renderExperience(content),
            renderProjects(content),
            renderEducation(content),
            renderSkillColumns(content, true)
        ));
    }

    private String baseHtml(ResumeTemplateDTO template, String body) {
        String accent = safeText(template.accentColor());
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>AI Resume</title>
              <style>
                * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                @page { size: A4; margin: 0; }
                html, body { margin: 0; padding: 0; background: #f3f4f6; }
                body {
                  font-family: "Noto Sans CJK SC", "NotoSansCJKsc-Regular", "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Songti SC", "Microsoft YaHei", Arial, sans-serif;
                  color: #172033;
                }
                .resume {
                  width: 210mm;
                  min-height: 297mm;
                  margin: 0 auto;
                  background: #ffffff;
                  padding: 16mm 14mm 14mm;
                }
                .dual-column {
                  display: grid;
                  grid-template-columns: 1.75fr 1fr;
                  gap: 10mm;
                }
                .single-column { display: block; }
                .minimal { background: linear-gradient(180deg, rgba(248,250,252,0.72), #ffffff 22%%); }
                .geek { background:
                  linear-gradient(180deg, rgba(15,23,42,0.035), rgba(15,23,42,0) 28%%),
                  #ffffff; }
                .fresh-blue-gray { background:
                  linear-gradient(180deg, rgba(91,124,153,0.08), rgba(91,124,153,0) 24%%),
                  #ffffff; }
                .header {
                  border-bottom: 2px solid %s;
                  padding-bottom: 10px;
                  margin-bottom: 12px;
                }
                .name {
                  font-size: 28px;
                  font-weight: 700;
                  margin: 0;
                  letter-spacing: 0;
                }
                .headline {
                  color: %s;
                  font-size: 14px;
                  font-weight: 600;
                  margin-top: 6px;
                }
                .contact {
                  margin-top: 8px;
                  font-size: 13px;
                  color: #475569;
                  display: flex;
                  flex-wrap: wrap;
                  gap: 10px;
                }
                .section {
                  margin-top: 14px;
                }
                .section-title {
                  font-size: 14px;
                  font-weight: 700;
                  color: %s;
                  margin: 0 0 8px;
                  padding-bottom: 4px;
                  border-bottom: 1px solid rgba(15, 23, 42, 0.12);
                }
                .entry {
                  margin-bottom: 10px;
                }
                .entry-header {
                  display: flex;
                  justify-content: space-between;
                  gap: 12px;
                  align-items: baseline;
                }
                .entry-title {
                  font-size: 14px;
                  font-weight: 700;
                }
                .entry-subtitle {
                  font-size: 13px;
                  color: #475569;
                  margin-top: 2px;
                }
                .entry-date {
                  font-size: 12px;
                  color: #64748b;
                  white-space: nowrap;
                }
                ul {
                  margin: 6px 0 0 18px;
                  padding: 0;
                }
                li {
                  font-size: 13px;
                  line-height: 1.45;
                  margin-bottom: 4px;
                }
                p {
                  font-size: 13px;
                  line-height: 1.45;
                  margin: 0;
                }
                .chips {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 6px;
                  margin-top: 6px;
                }
                .chip {
                  border: 1px solid rgba(15, 23, 42, 0.12);
                  border-radius: 999px;
                  padding: 3px 8px;
                  font-size: 12px;
                  color: #334155;
                }
              </style>
            </head>
            <body>
              %s
            </body>
            </html>
            """.formatted(accent, accent, accent, body);
    }

    private String renderHeader(ResumeGeneratedContentDTO content) {
        ResumeProfileDTO.BasicInfoDTO basicInfo = content.basicInfo();
        StringJoiner contact = new StringJoiner(" ");
        addIfPresent(contact, safeText(basicInfo.phone()));
        addIfPresent(contact, safeText(basicInfo.email()));
        addIfPresent(contact, safeText(basicInfo.city()));
        addIfPresent(contact, safeText(basicInfo.personalSite()));
        return """
            <section class="header">
              <h1 class="name">%s</h1>
              <div class="headline">%s</div>
              <div class="contact">%s</div>
            </section>
            """.formatted(
            safeText(basicInfo.name()),
            safeText(content.headline()),
            safeText(contact.toString())
        );
    }

    private String renderSummary(ResumeGeneratedContentDTO content) {
        if (content.summaryHighlights() == null || content.summaryHighlights().isEmpty()) {
            return "";
        }
        return """
            <section class="section">
              <h2 class="section-title">个人亮点</h2>
              %s
            </section>
            """.formatted(renderBulletList(content.summaryHighlights()));
    }

    private String renderEducation(ResumeGeneratedContentDTO content) {
        if (content.educations() == null || content.educations().isEmpty()) {
            return "";
        }
        String entries = content.educations().stream()
            .map(education -> """
                <div class="entry">
                  <div class="entry-header">
                    <div class="entry-title">%s</div>
                    <div class="entry-date">%s - %s</div>
                  </div>
                  <div class="entry-subtitle">%s | %s</div>
                  %s
                </div>
                """.formatted(
                safeText(education.school()),
                safeText(education.startDate()),
                safeText(education.endDate()),
                safeText(education.degree()),
                safeText(education.major()),
                renderBulletList(education.highlights())
            ))
            .reduce("", String::concat);
        return wrapSection("教育经历", entries);
    }

    private String renderExperience(ResumeGeneratedContentDTO content) {
        if (content.experiences() == null || content.experiences().isEmpty()) {
            return "";
        }
        String entries = content.experiences().stream()
            .map(experience -> """
                <div class="entry">
                  <div class="entry-header">
                    <div class="entry-title">%s</div>
                    <div class="entry-date">%s - %s</div>
                  </div>
                  <div class="entry-subtitle">%s</div>
                  %s
                </div>
                """.formatted(
                safeText(experience.company()),
                safeText(experience.startDate()),
                safeText(experience.endDate()),
                safeText(experience.title()),
                renderBulletList(experience.highlights())
            ))
            .reduce("", String::concat);
        return wrapSection("工作经历", entries);
    }

    private String renderProjects(ResumeGeneratedContentDTO content) {
        if (content.projects() == null || content.projects().isEmpty()) {
            return "";
        }
        String entries = content.projects().stream()
            .map(project -> """
                <div class="entry">
                  <div class="entry-header">
                    <div class="entry-title">%s</div>
                    <div class="entry-date">%s - %s</div>
                  </div>
                  <div class="entry-subtitle">%s</div>
                  %s
                  %s
                </div>
                """.formatted(
                safeText(project.name()),
                safeText(project.startDate()),
                safeText(project.endDate()),
                safeText(project.role()),
                renderTechStack(project.techStack()),
                renderBulletList(project.highlights())
            ))
            .reduce("", String::concat);
        return wrapSection("项目经历", entries);
    }

    private String renderSkillColumns(ResumeGeneratedContentDTO content, boolean compact) {
        StringBuilder body = new StringBuilder();
        if (content.skillCategories() != null) {
            content.skillCategories().forEach(category -> body.append("""
                <div class="entry">
                  <div class="entry-title">%s</div>
                  <div class="chips">%s</div>
                </div>
                """.formatted(
                safeText(category.category()),
                (category.items() == null ? List.<String>of() : category.items()).stream()
                    .map(item -> "<span class=\"chip\">" + safeText(item) + "</span>")
                    .reduce("", String::concat)
            )));
        }
        if (content.awards() != null && !content.awards().isEmpty()) {
            body.append(renderInlineListSection("奖项证书", content.awards(), compact));
        }
        if (content.extraSections() != null) {
            content.extraSections().forEach(section -> body.append(renderInlineListSection(
                section.title(),
                section.items(),
                compact
            )));
        }
        return wrapSection("技能与补充", body.toString());
    }

    private String renderInlineListSection(String title, List<String> items, boolean compact) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        String content = compact
            ? renderBulletList(items)
            : "<div class=\"chips\">"
                + items.stream().map(item -> "<span class=\"chip\">" + safeText(item) + "</span>").reduce("", String::concat)
                + "</div>";
        return """
            <div class="entry">
              <div class="entry-title">%s</div>
              %s
            </div>
            """.formatted(safeText(title), content);
    }

    private String renderTechStack(List<String> techStack) {
        if (techStack == null || techStack.isEmpty()) {
            return "";
        }
        return """
            <div class="chips">%s</div>
            """.formatted(
            techStack.stream()
                .map(item -> "<span class=\"chip\">" + safeText(item) + "</span>")
                .reduce("", String::concat)
        );
    }

    private String wrapSection(String title, String body) {
        if (!notBlank(body)) {
            return "";
        }
        return """
            <section class="section">
              <h2 class="section-title">%s</h2>
              %s
            </section>
            """.formatted(safeText(title), body);
    }

    private String renderBulletList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        String entries = items.stream()
            .filter(this::notBlank)
            .map(item -> "<li>" + safeText(item) + "</li>")
            .reduce("", String::concat);
        return notBlank(entries) ? "<ul>" + entries + "</ul>" : "";
    }

    private void addIfPresent(StringJoiner joiner, String value) {
        if (notBlank(value)) {
            joiner.add(value);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value.trim());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, ResumeTemplateDTO> loadTemplates(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        try {
            String json = resourceLoader
                .getResource("classpath:resume-maker/builtin-templates.json")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            List<ResumeTemplateDTO> loadedTemplates = objectMapper.readValue(
                json,
                new TypeReference<List<ResumeTemplateDTO>>() {
                }
            );
            Map<String, ResumeTemplateDTO> templateMap = new LinkedHashMap<>();
            for (ResumeTemplateDTO template : loadedTemplates) {
                templateMap.put(template.id(), template);
            }
            if (templateMap.isEmpty()) {
                log.warn("内置简历模板清单读取成功，但内容为空");
            }
            log.info("加载内置简历模板成功: count={}", templateMap.size());
            return templateMap;
        } catch (IOException e) {
            log.error("加载内置简历模板失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加载内置简历模板失败");
        }
    }
}
