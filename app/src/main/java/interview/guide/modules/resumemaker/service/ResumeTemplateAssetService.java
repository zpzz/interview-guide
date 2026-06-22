package interview.guide.modules.resumemaker.service;

import interview.guide.common.auth.CurrentUserService;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.infrastructure.file.DocumentParseService;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.resumemaker.dto.ResumeTemplateAssetDTO;
import interview.guide.modules.resumemaker.model.ResumeTemplateAssetEntity;
import interview.guide.modules.resumemaker.repository.ResumeTemplateAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTemplateAssetService {

    private static final long MAX_TEMPLATE_FILE_SIZE = 20 * 1024 * 1024;

    private final ResumeTemplateAssetRepository assetRepository;
    private final CurrentUserService currentUserService;
    private final FileValidationService fileValidationService;
    private final FileStorageService fileStorageService;
    private final DocumentParseService documentParseService;

    public List<ResumeTemplateAssetDTO> listAssets() {
        Long userId = currentUserService.currentUserId();
        return assetRepository.findAllByUserIdOrderByUploadedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public ResumeTemplateAssetDTO uploadTemplate(MultipartFile file) {
        fileValidationService.validateFile(file, MAX_TEMPLATE_FILE_SIZE, "简历模板");

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        fileValidationService.validateContentType(
            contentType,
            fileName,
            this::isPdfMimeType,
            this::isPdfExtension,
            "目前仅支持上传 PDF 格式的简历模板"
        );

        String storageKey = fileStorageService.uploadResumeTemplate(file);
        String storageUrl = fileStorageService.getFileUrl(storageKey);

        ResumeTemplateAssetEntity entity = ResumeTemplateAssetEntity.builder()
            .userId(currentUserService.currentUserId())
            .originalFilename(fileName == null || fileName.isBlank() ? "template.pdf" : fileName)
            .contentType(contentType == null || contentType.isBlank() ? "application/pdf" : contentType)
            .fileSize(file.getSize())
            .storageKey(storageKey)
            .storageUrl(storageUrl)
            .build();
        ResumeTemplateAssetEntity saved = assetRepository.save(entity);
        log.info("简历模板上传成功: assetId={}, userId={}", saved.getId(), saved.getUserId());
        return toDto(saved);
    }

    @Transactional
    public void deleteAsset(Long id) {
        ResumeTemplateAssetEntity entity = loadOwnedAsset(id);
        assetRepository.delete(entity);
        fileStorageService.deleteResumeTemplate(entity.getStorageKey());
    }

    public String buildTemplateReferenceHtml(Long assetId) {
        ResumeTemplateAssetEntity entity = loadOwnedAsset(assetId);
        byte[] fileBytes = fileStorageService.downloadFile(entity.getStorageKey());
        String parsedText = documentParseService.parseContent(fileBytes, entity.getOriginalFilename());
        if (parsedText == null || parsedText.isBlank()) {
            throw new BusinessException(ErrorCode.RESUME_TEMPLATE_NOT_FOUND, "模板内容解析结果为空");
        }
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <title>%s</title>
              <style>
                body { font-family: "Noto Sans CJK SC", "PingFang SC", "Microsoft YaHei", Arial, sans-serif; padding: 32px; color: #111827; line-height: 1.6; }
                h1 { font-size: 20px; margin: 0 0 16px; }
                pre { white-space: pre-wrap; word-break: break-word; font-size: 14px; margin: 0; }
              </style>
            </head>
            <body>
              <h1>模板参考：%s</h1>
              <pre>%s</pre>
            </body>
            </html>
            """.formatted(
            HtmlUtils.htmlEscape(entity.getOriginalFilename()),
            HtmlUtils.htmlEscape(entity.getOriginalFilename()),
            HtmlUtils.htmlEscape(parsedText)
        );
    }

    private ResumeTemplateAssetEntity loadOwnedAsset(Long id) {
        return assetRepository.findByIdAndUserId(id, currentUserService.currentUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_TEMPLATE_NOT_FOUND, "简历模板文件不存在"));
    }

    private ResumeTemplateAssetDTO toDto(ResumeTemplateAssetEntity entity) {
        return new ResumeTemplateAssetDTO(
            entity.getId(),
            entity.getOriginalFilename(),
            entity.getContentType(),
            entity.getFileSize(),
            entity.getStorageUrl(),
            entity.getUploadedAt()
        );
    }

    private boolean isPdfMimeType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("pdf");
    }

    private boolean isPdfExtension(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }
}
