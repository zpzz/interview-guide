package interview.guide.modules.resumemaker;

import interview.guide.common.result.Result;
import interview.guide.modules.resumemaker.dto.GeneratedResumeDetailDTO;
import interview.guide.modules.resumemaker.dto.GeneratedResumeListItemDTO;
import interview.guide.modules.resumemaker.dto.GenerateResumePreviewRequest;
import interview.guide.modules.resumemaker.dto.GenerateResumePreviewResponse;
import interview.guide.modules.resumemaker.dto.ResumeTemplateAssetDTO;
import interview.guide.modules.resumemaker.dto.ResumeTemplateDTO;
import interview.guide.modules.resumemaker.service.ResumeMakerService;
import interview.guide.modules.resumemaker.service.ResumeTemplateAssetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "AI 简历编写", description = "上传简历后进行 AI 优化和 HTML 预览")
public class ResumeMakerController {

    private final ResumeMakerService resumeMakerService;
    private final ResumeTemplateAssetService resumeTemplateAssetService;

    @GetMapping("/api/resume-maker/templates/builtin")
    public Result<List<ResumeTemplateDTO>> listBuiltinTemplates() {
        return Result.success(resumeMakerService.listTemplates());
    }

    @GetMapping("/api/resume-maker/template-assets")
    public Result<List<ResumeTemplateAssetDTO>> listTemplateAssets() {
        return Result.success(resumeTemplateAssetService.listAssets());
    }

    @PostMapping("/api/resume-maker/template-assets/upload")
    public Result<ResumeTemplateAssetDTO> uploadTemplateAsset(@RequestParam("file") MultipartFile file) {
        return Result.success(resumeTemplateAssetService.uploadTemplate(file));
    }

    @DeleteMapping("/api/resume-maker/template-assets/{id}")
    public Result<Void> deleteTemplateAsset(@PathVariable Long id) {
        resumeTemplateAssetService.deleteAsset(id);
        return Result.success(null);
    }

    @PostMapping("/api/resume-maker/preview/generate")
    public Result<GenerateResumePreviewResponse> generatePreview(@Valid @RequestBody GenerateResumePreviewRequest request) {
        return Result.success(resumeMakerService.generatePreview(request, false));
    }

    @PostMapping("/api/resume-maker/preview/optimize-by-jd")
    public Result<GenerateResumePreviewResponse> optimizePreview(@Valid @RequestBody GenerateResumePreviewRequest request) {
        return Result.success(resumeMakerService.generatePreview(request, true));
    }

    @GetMapping("/api/resume-maker/generated-resumes")
    public Result<List<GeneratedResumeListItemDTO>> listGeneratedResumes() {
        return Result.success(resumeMakerService.listGeneratedResumes());
    }

    @GetMapping("/api/resume-maker/generated-resumes/{id}")
    public Result<GeneratedResumeDetailDTO> getGeneratedResume(@PathVariable Long id) {
        return Result.success(resumeMakerService.getGeneratedResume(id));
    }

    @DeleteMapping("/api/resume-maker/generated-resumes/{id}")
    public Result<Void> deleteGeneratedResume(@PathVariable Long id) {
        resumeMakerService.deleteGeneratedResume(id);
        return Result.success(null);
    }
}
