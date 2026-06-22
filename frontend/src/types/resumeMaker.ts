export interface ResumeTemplate {
  id: string;
  name: string;
  description: string;
  accentColor: string;
  layoutType: string;
  recommendedScenario: string;
  previewImageUrl: string;
  referenceHtmlUrl: string;
  referencePdfUrl: string;
}

export interface ResumeBasicInfo {
  name: string;
  phone: string;
  email: string;
  city: string;
  targetTitle: string;
  personalSite: string;
}

export interface ResumeSkillCategory {
  category: string;
  items: string[];
}

export interface ResumeGeneratedEducation {
  school: string;
  degree: string;
  major: string;
  startDate: string;
  endDate: string;
  highlights: string[];
}

export interface ResumeGeneratedExperience {
  company: string;
  title: string;
  startDate: string;
  endDate: string;
  highlights: string[];
}

export interface ResumeGeneratedProject {
  name: string;
  role: string;
  startDate: string;
  endDate: string;
  techStack: string[];
  highlights: string[];
}

export interface ResumeExtraSection {
  title: string;
  items: string[];
}

export interface ResumeGeneratedContent {
  basicInfo: ResumeBasicInfo;
  headline: string;
  summaryHighlights: string[];
  educations: ResumeGeneratedEducation[];
  experiences: ResumeGeneratedExperience[];
  projects: ResumeGeneratedProject[];
  skillCategories: ResumeSkillCategory[];
  awards: string[];
  extraSections: ResumeExtraSection[];
}

export interface GenerateResumePreviewRequest {
  sourceResumeId: number;
  sourceResumeName: string;
  title: string;
  targetRole: string;
  jdText: string;
  extraNotes: string;
  targetPageCount: number;
  builtinTemplateId?: string;
  templateAssetId?: number;
  providerId?: string;
}

export interface GenerateResumePreviewResponse {
  generatedResumeId: number;
  sourceResumeId: number;
  sourceResumeName: string;
  title: string;
  templateId: string;
  htmlContent: string;
  generatedContent: ResumeGeneratedContent | null;
}

export interface GeneratedResumeListItem {
  id: number;
  title: string;
  sourceResumeId: number;
  sourceResumeName: string;
  targetRole: string;
  builtinTemplateId?: string | null;
  templateAssetId?: number | null;
  targetPageCount?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface GeneratedResumeDetail {
  id: number;
  sourceResumeId: number;
  sourceResumeName: string;
  title: string;
  targetRole: string;
  jdText: string;
  builtinTemplateId?: string | null;
  templateAssetId?: number | null;
  providerId?: string | null;
  targetPageCount?: number | null;
  htmlContent: string;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeTemplateAsset {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  storageUrl: string;
  uploadedAt: string;
}
