package interview.guide.modules.resumemaker.service;

import interview.guide.common.auth.CurrentUserService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.service.ResumePersistenceService;
import interview.guide.modules.resumemaker.dto.GeneratedResumeDetailDTO;
import interview.guide.modules.resumemaker.dto.GeneratedResumeListItemDTO;
import interview.guide.modules.resumemaker.dto.GenerateResumePreviewRequest;
import interview.guide.modules.resumemaker.dto.GenerateResumePreviewResponse;
import interview.guide.modules.resumemaker.dto.ResumeProfileDTO;
import interview.guide.modules.resumemaker.dto.ResumeTemplateDTO;
import interview.guide.modules.resumemaker.model.GeneratedResumeEntity;
import interview.guide.modules.resumemaker.repository.GeneratedResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMakerService {

    private final GeneratedResumeRepository generatedResumeRepository;
    private final ResumeMakerTemplateService templateService;
    private final ResumeMakerAiService aiService;
    private final CurrentUserService currentUserService;
    private final ResumePersistenceService resumePersistenceService;
    private final ResumeWritingProfileService profileService;
    private final ResumeTemplateAssetService resumeTemplateAssetService;

    public List<ResumeTemplateDTO> listTemplates() {
        return templateService.listTemplates();
    }

    public GenerateResumePreviewResponse generatePreview(GenerateResumePreviewRequest request, boolean optimize) {
        ResumeEntity resume = resumePersistenceService.findById(request.sourceResumeId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND, "原始简历不存在"));
        ResumeProfileDTO profile = profileService.buildProfileFromResume(resume);
        String templateReferenceHtml = buildTemplateReferenceHtml(
            request.builtinTemplateId(),
            request.templateAssetId()
        );
        String htmlContent = aiService.generateHtml(
            profile,
            buildExportTitle(request.title(), request.sourceResumeName(), request.targetRole()),
            request.targetRole(),
            request.jdText(),
            request.extraNotes(),
            request.targetPageCount(),
            templateReferenceHtml,
            currentUserService.currentUserId(),
            request.providerId()
        );
        String finalTitle = buildExportTitle(request.title(), request.sourceResumeName(), request.targetRole());
        GeneratedResumeEntity saved = saveGeneratedResume(request, finalTitle, htmlContent);
        String templateId = request.builtinTemplateId() != null && !request.builtinTemplateId().isBlank()
            ? request.builtinTemplateId()
            : null;
        return new GenerateResumePreviewResponse(
            saved.getId(),
            request.sourceResumeId(),
            request.sourceResumeName(),
            finalTitle,
            templateId,
            htmlContent,
            null
        );
    }

    @Transactional
    protected GeneratedResumeEntity saveGeneratedResume(GenerateResumePreviewRequest request,
                                                        String finalTitle,
                                                        String htmlContent) {
        GeneratedResumeEntity entity = GeneratedResumeEntity.builder()
            .userId(currentUserService.currentUserId())
            .sourceResumeId(request.sourceResumeId())
            .sourceResumeName(request.sourceResumeName())
            .title(finalTitle)
            .targetRole(request.targetRole())
            .jdText(request.jdText())
            .providerId(request.providerId())
            .targetPageCount(request.targetPageCount())
            .builtinTemplateId(request.builtinTemplateId())
            .templateAssetId(request.templateAssetId())
            .htmlContent(htmlContent)
            .build();
        return generatedResumeRepository.save(entity);
    }

    private String buildTemplateReferenceHtml(String builtinTemplateId, Long templateAssetId) {
        if (builtinTemplateId != null && !builtinTemplateId.isBlank()) {
            return templateService.getReferenceHtml(builtinTemplateId);
        }
        if (templateAssetId == null) {
            return "";
        }
        return resumeTemplateAssetService.buildTemplateReferenceHtml(templateAssetId);
    }

    public List<GeneratedResumeListItemDTO> listGeneratedResumes() {
        List<GeneratedResumeEntity> records = currentUserService.isAdmin()
            ? generatedResumeRepository.findAll().stream()
                .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .toList()
            : generatedResumeRepository.findAllByUserIdOrderByUpdatedAtDesc(currentUserService.currentUserId());
        return records.stream().map(this::toListItem).toList();
    }

    public GeneratedResumeDetailDTO getGeneratedResume(Long id) {
        return toDetail(loadGeneratedResume(id));
    }

    @Transactional
    public void deleteGeneratedResume(Long id) {
        GeneratedResumeEntity entity = loadGeneratedResume(id);
        generatedResumeRepository.delete(entity);
    }

    private GeneratedResumeEntity loadGeneratedResume(Long id) {
        if (currentUserService.isAdmin()) {
            return generatedResumeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND, "生成简历记录不存在"));
        }
        return generatedResumeRepository.findByIdAndUserId(id, currentUserService.currentUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND, "生成简历记录不存在"));
    }

    private GeneratedResumeListItemDTO toListItem(GeneratedResumeEntity entity) {
        return new GeneratedResumeListItemDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getSourceResumeId(),
            entity.getSourceResumeName(),
            entity.getTargetRole(),
            entity.getBuiltinTemplateId(),
            entity.getTemplateAssetId(),
            entity.getTargetPageCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private GeneratedResumeDetailDTO toDetail(GeneratedResumeEntity entity) {
        return new GeneratedResumeDetailDTO(
            entity.getId(),
            entity.getSourceResumeId(),
            entity.getSourceResumeName(),
            entity.getTitle(),
            entity.getTargetRole(),
            entity.getJdText(),
            entity.getBuiltinTemplateId(),
            entity.getTemplateAssetId(),
            entity.getProviderId(),
            entity.getTargetPageCount(),
            entity.getHtmlContent(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String buildExportTitle(String title, String sourceResumeName, String targetRole) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String baseName = sourceResumeName == null ? "resume" : sourceResumeName.replaceFirst("\\.[^.]+$", "");
        if (targetRole != null && !targetRole.isBlank()) {
            return baseName + "-" + targetRole.trim() + "-优化版";
        }
        return baseName + "-优化版";
    }
}
