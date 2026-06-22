import request from './request';
import type {
  GenerateResumePreviewRequest,
  GenerateResumePreviewResponse,
  GeneratedResumeDetail,
  GeneratedResumeListItem,
  ResumeTemplateAsset,
  ResumeTemplate,
} from '../types/resumeMaker';

export const resumeMakerApi = {
  listBuiltinTemplates: () => request.get<ResumeTemplate[]>('/api/resume-maker/templates/builtin'),

  listTemplateAssets: () => request.get<ResumeTemplateAsset[]>('/api/resume-maker/template-assets'),

  async uploadTemplateAsset(file: File): Promise<ResumeTemplateAsset> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<ResumeTemplateAsset>('/api/resume-maker/template-assets/upload', formData);
  },

  deleteTemplateAsset: (id: number) => request.delete<void>(`/api/resume-maker/template-assets/${id}`),

  listGeneratedResumes: () => request.get<GeneratedResumeListItem[]>('/api/resume-maker/generated-resumes'),

  getGeneratedResume: (id: number) => request.get<GeneratedResumeDetail>(`/api/resume-maker/generated-resumes/${id}`),

  deleteGeneratedResume: (id: number) => request.delete<void>(`/api/resume-maker/generated-resumes/${id}`),

  generatePreview: (data: GenerateResumePreviewRequest) =>
    request.post<GenerateResumePreviewResponse>('/api/resume-maker/preview/generate', data, {
      timeout: 300000,
    }),

  optimizePreview: (data: GenerateResumePreviewRequest) =>
    request.post<GenerateResumePreviewResponse>('/api/resume-maker/preview/optimize-by-jd', data, {
      timeout: 300000,
    }),
};
