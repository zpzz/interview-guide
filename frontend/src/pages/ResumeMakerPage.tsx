import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ChevronDown,
  ExternalLink,
  Download,
  FileText,
  FolderOpen,
  CodeXml,
  Loader2,
  PencilLine,
  Sparkles,
  SquarePen,
  Trash2,
  Upload,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import { getApiBaseUrl, getErrorMessage } from '../api/request';
import { llmProviderApi } from '../api/llmProvider';
import { historyApi, type ResumeDetail, type ResumeListItem } from '../api/history';
import { resumeMakerApi } from '../api/resumeMaker';
import type { ProviderItem } from '../types/llmProvider';
import type {
  GenerateResumePreviewResponse,
  GeneratedResumeListItem,
  ResumeTemplate,
  ResumeTemplateAsset,
} from '../types/resumeMaker';

const sectionCardClass = 'rounded-2xl border border-slate-200/80 bg-white/90 p-5 pr-5 shadow-sm backdrop-blur-sm dark:border-slate-700 dark:bg-slate-900/70';
const workspacePanelClass = 'flex h-[1120px] min-h-[1120px] flex-col';
const previewViewportClass = 'h-[980px] shrink-0 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 dark:border-slate-700 dark:bg-slate-950';
const inputClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:focus:ring-primary-900/30';
const textareaClass = `${inputClass} resize-y`;
const apiBaseUrl = getApiBaseUrl();

export default function ResumeMakerPage() {
  const [activeTab, setActiveTab] = useState<'editor' | 'history'>('editor');
  const [providers, setProviders] = useState<ProviderItem[]>([]);
  const [resumeOptions, setResumeOptions] = useState<ResumeListItem[]>([]);
  const [builtinTemplates, setBuiltinTemplates] = useState<ResumeTemplate[]>([]);
  const [templateAssets, setTemplateAssets] = useState<ResumeTemplateAsset[]>([]);
  const [generatedResumes, setGeneratedResumes] = useState<GeneratedResumeListItem[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState<number | null>(null);
  const [selectedResumeName, setSelectedResumeName] = useState('');
  const [selectedGeneratedResumeId, setSelectedGeneratedResumeId] = useState<number | null>(null);
  const [selectedBuiltinTemplateId, setSelectedBuiltinTemplateId] = useState('');
  const [selectedTemplateAssetId, setSelectedTemplateAssetId] = useState<number | null>(null);
  const [resumePreviewText, setResumePreviewText] = useState('');
  const [title, setTitle] = useState('新建简历');
  const [targetRole, setTargetRole] = useState('');
  const [jdText, setJdText] = useState('');
  const [extraNotes, setExtraNotes] = useState('');
  const [targetPageCount, setTargetPageCount] = useState('1');
  const [providerId, setProviderId] = useState('');
  const [htmlPreview, setHtmlPreview] = useState('');
  const [generatedResult, setGeneratedResult] = useState<GenerateResumePreviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [uploadingTemplate, setUploadingTemplate] = useState(false);
  const [deletingTemplateId, setDeletingTemplateId] = useState<number | null>(null);
  const [deletingGeneratedResumeId, setDeletingGeneratedResumeId] = useState<number | null>(null);
  const [generatedResumeToDelete, setGeneratedResumeToDelete] = useState<GeneratedResumeListItem | null>(null);
  const [loadingGeneratedResumeId, setLoadingGeneratedResumeId] = useState<number | null>(null);
  const [builtinTemplatesExpanded, setBuiltinTemplatesExpanded] = useState(false);
  const [error, setError] = useState('');
  const templateInputRef = useRef<HTMLInputElement | null>(null);
  const previewFrameRef = useRef<HTMLIFrameElement | null>(null);

  const providerOptions = useMemo(
    () => providers.map(item => ({ value: item.id, label: item.id })),
    [providers],
  );

  const toBackendAssetUrl = (path: string) => {
    if (!path) {
      return '';
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    return `${apiBaseUrl}${path}`;
  };

  useEffect(() => {
    void loadInitialData();
  }, []);

  const loadInitialData = async () => {
    setLoading(true);
    setError('');
    try {
      const [providerData, resumeData, templateData, builtinTemplateData, generatedResumeData] = await Promise.all([
        llmProviderApi.list(),
        historyApi.getResumes(),
        resumeMakerApi.listTemplateAssets(),
        resumeMakerApi.listBuiltinTemplates(),
        resumeMakerApi.listGeneratedResumes(),
      ]);
      setProviders(providerData);
      setResumeOptions(resumeData);
      setTemplateAssets(templateData);
      setBuiltinTemplates(builtinTemplateData);
      setGeneratedResumes(generatedResumeData);
      if (templateData.length > 0) {
        setSelectedTemplateAssetId(templateData[0].id);
      }
      if (resumeData.length > 0) {
        await applyResumeSource(resumeData[0].id, resumeData[0].filename, true);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const loadGeneratedResume = async (id: number) => {
    setLoadingGeneratedResumeId(id);
    setError('');
    try {
      const detail = await resumeMakerApi.getGeneratedResume(id);
      setSelectedGeneratedResumeId(detail.id);
      setSelectedResumeId(detail.sourceResumeId);
      setSelectedResumeName(detail.sourceResumeName);
      setTitle(detail.title);
      setTargetRole(detail.targetRole ?? '');
      setJdText(detail.jdText ?? '');
      setExtraNotes('');
      setTargetPageCount(String(detail.targetPageCount ?? 1));
      setSelectedBuiltinTemplateId(detail.builtinTemplateId ?? '');
      setSelectedTemplateAssetId(detail.templateAssetId ?? null);
      setProviderId(detail.providerId ?? '');
      setHtmlPreview(detail.htmlContent);
      setGeneratedResult({
        generatedResumeId: detail.id,
        sourceResumeId: detail.sourceResumeId,
        sourceResumeName: detail.sourceResumeName,
        title: detail.title,
        templateId: detail.builtinTemplateId ?? '',
        htmlContent: detail.htmlContent,
        generatedContent: null,
      });
      const sourceResumeDetail = await historyApi.getResumeDetail(detail.sourceResumeId);
      applyResumeDetail(sourceResumeDetail, false, { resetPreview: false });
      setSelectedResumeName(detail.sourceResumeName);
      setActiveTab('history');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoadingGeneratedResumeId(null);
    }
  };

  const handleDeleteGeneratedResume = async (id: number) => {
    setDeletingGeneratedResumeId(id);
    setError('');
    try {
      await resumeMakerApi.deleteGeneratedResume(id);
      setGeneratedResumes((prev) => prev.filter((item) => item.id !== id));
      if (selectedGeneratedResumeId === id) {
        setSelectedGeneratedResumeId(null);
        setGeneratedResult(null);
        setHtmlPreview('');
      }
      setGeneratedResumeToDelete(null);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingGeneratedResumeId(null);
    }
  };

  const handleExportHtml = () => {
    if (!htmlPreview) {
      setError('请先生成或查看一份简历预览');
      return;
    }
    try {
      const blob = new Blob([htmlPreview], { type: 'text/html;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${title || 'resume'}.html`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const applyResumeDetail = (
    detail: ResumeDetail,
    overwriteTitle = false,
    options?: { resetPreview?: boolean },
  ) => {
    setSelectedResumeId(detail.id);
    setSelectedResumeName(detail.filename);
    setResumePreviewText(detail.resumeText ?? '');
    if (overwriteTitle || title === '新建简历' || !title.trim()) {
      const normalizedTitle = detail.filename.replace(/\.[^.]+$/, '');
      setTitle(`${normalizedTitle}-优化版`);
    }
    if (options?.resetPreview !== false) {
      setSelectedGeneratedResumeId(null);
      setGeneratedResult(null);
      setHtmlPreview('');
    }
  };

  const applyResumeSource = async (resumeId: number, resumeName?: string, overwriteTitle = false) => {
    setError('');
    try {
      const detail = await historyApi.getResumeDetail(resumeId);
      applyResumeDetail(detail, overwriteTitle);
      if (resumeName) {
        setSelectedResumeName(resumeName);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const buildPayload = () => {
    if (!selectedResumeId || !selectedResumeName) {
      throw new Error('请先选择一份已上传简历');
    }
    return {
      sourceResumeId: selectedResumeId,
      sourceResumeName: selectedResumeName,
      title,
      targetRole,
      jdText,
      extraNotes,
      targetPageCount: normalizeTargetPageCount(targetPageCount),
      builtinTemplateId: selectedBuiltinTemplateId || undefined,
      templateAssetId: selectedTemplateAssetId ?? undefined,
      providerId: providerId || undefined,
    };
  };

  const handleTemplateUpload = async (file?: File) => {
    if (!file) {
      return;
    }
    setUploadingTemplate(true);
    setError('');
    try {
      const asset = await resumeMakerApi.uploadTemplateAsset(file);
      setTemplateAssets((prev) => [asset, ...prev]);
      setSelectedBuiltinTemplateId('');
      setSelectedTemplateAssetId(asset.id);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setUploadingTemplate(false);
      if (templateInputRef.current) {
        templateInputRef.current.value = '';
      }
    }
  };

  const handleDeleteTemplate = async (assetId: number) => {
    setDeletingTemplateId(assetId);
    setError('');
    try {
      await resumeMakerApi.deleteTemplateAsset(assetId);
      setTemplateAssets((prev) => prev.filter((item) => item.id !== assetId));
      setSelectedTemplateAssetId((prev) => (prev === assetId ? null : prev));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingTemplateId(null);
    }
  };

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    setSelectedGeneratedResumeId(null);
    setGeneratedResult(null);
    setHtmlPreview('');
    try {
      const result = await resumeMakerApi.generatePreview(buildPayload());
      setGeneratedResult(result);
      setSelectedGeneratedResumeId(result.generatedResumeId);
      setHtmlPreview(result.htmlContent);
      setTitle(result.title);
      const generatedList = await resumeMakerApi.listGeneratedResumes();
      setGeneratedResumes(generatedList);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setGenerating(false);
    }
  };

  const handleExport = async () => {
    if (!htmlPreview) {
      setError('请先生成简历预览');
      return;
    }
    setExporting(true);
    setError('');
    try {
      await printHtmlPreview(htmlPreview);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setExporting(false);
    }
  };

  const handlePreviewFrameLoad = () => {
    const iframeWindow = previewFrameRef.current?.contentWindow;
    const iframeDocument = previewFrameRef.current?.contentDocument;
    if (!iframeWindow || !iframeDocument) {
      return;
    }
    const multiPageLayout = iframeDocument.querySelectorAll('.page').length > 1;

    applyPreviewStyles(iframeDocument);
    applyTemplateSpecificPreviewAdjustments(iframeDocument, generatedResult?.templateId);
    fitPreviewToViewport(iframeWindow, iframeDocument);
    resetPreviewScrollPosition(iframeWindow, iframeDocument);

    iframeWindow.requestAnimationFrame(() => {
      if (!multiPageLayout) {
        normalizePreviewTopOverflow(iframeWindow, iframeDocument);
      }
      fitPreviewToViewport(iframeWindow, iframeDocument);
      resetPreviewScrollPosition(iframeWindow, iframeDocument);
      iframeWindow.requestAnimationFrame(() => {
        if (!multiPageLayout) {
          normalizePreviewTopOverflow(iframeWindow, iframeDocument);
        }
        fitPreviewToViewport(iframeWindow, iframeDocument);
        resetPreviewScrollPosition(iframeWindow, iframeDocument);
      });
    });
  };

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-900 dark:text-white">
            <PencilLine className="h-7 w-7 text-primary-500" />
            AI 简历编写
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            上传简历后，Java 负责传递解析文本，并结合模板参考生成 HTML 简历，支持在线预览和浏览器打印导出 PDF。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button onClick={handleGenerate} disabled={generating || !selectedResumeId} className="btn-primary flex items-center gap-2 rounded-xl px-4 py-2 text-sm">
            {generating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
            生成简历
          </button>
          <button onClick={handleExportHtml} disabled={!htmlPreview} className="btn-secondary flex items-center gap-2 rounded-xl px-4 py-2 text-sm">
            <CodeXml className="h-4 w-4" />
            导出 HTML
          </button>
          <button onClick={handleExport} disabled={exporting || !htmlPreview} className="btn-secondary flex items-center gap-2 rounded-xl px-4 py-2 text-sm">
            {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
            导出 PDF
          </button>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-300">
          {error}
        </div>
      )}

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
        <section className={`${sectionCardClass} ${workspacePanelClass}`}>
          <div className="mb-4 inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1 dark:border-slate-700 dark:bg-slate-900/60">
            <button
              type="button"
              onClick={() => setActiveTab('editor')}
              className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                activeTab === 'editor'
                  ? 'bg-white text-primary-600 shadow-sm dark:bg-slate-800 dark:text-primary-300'
                  : 'text-slate-500 hover:text-slate-800 dark:text-slate-300 dark:hover:text-white'
              }`}
            >
              <SquarePen className="h-4 w-4" />
              生成配置
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('history')}
              className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                activeTab === 'history'
                  ? 'bg-white text-primary-600 shadow-sm dark:bg-slate-800 dark:text-primary-300'
                  : 'text-slate-500 hover:text-slate-800 dark:text-slate-300 dark:hover:text-white'
              }`}
            >
              <FolderOpen className="h-4 w-4" />
              已生成简历
            </button>
          </div>

          <div className="resume-maker-scrollbar min-h-0 flex-1 overflow-y-auto pr-2">
          {activeTab === 'editor' ? (
          <>
            <div className="mb-4">
              <h2 className="text-sm font-semibold text-slate-900 dark:text-white">生成配置</h2>
            </div>

            <div className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <InputField label="简历标题" value={title} onChange={setTitle} />
              <InputField label="目标岗位（可选）" value={targetRole} onChange={setTargetRole} />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <InputField
                label="目标页数"
                value={targetPageCount}
                onChange={(value) => setTargetPageCount(value.replace(/[^\d]/g, '').slice(0, 2))}
              />
              <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-3 text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-900/40 dark:text-slate-400">
                填写希望生成后的页数，例如 `1`、`2`、`3`。这个值会直接传给大模型做版面控制。
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">选择已上传简历</span>
                <select
                  value={selectedResumeId ?? ''}
                  onChange={(event) => {
                    const nextId = Number(event.target.value);
                    const nextResume = resumeOptions.find(item => item.id === nextId);
                    if (!nextResume) {
                      return;
                    }
                    void applyResumeSource(nextResume.id, nextResume.filename, true);
                  }}
                  className={inputClass}
                >
                  <option value="">请选择</option>
                  {resumeOptions.map(item => (
                    <option key={item.id} value={item.id}>{item.filename}</option>
                  ))}
                </select>
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">简历模板 PDF（可选）</span>
                <select
                  value={
                    selectedBuiltinTemplateId
                      ? `builtin:${selectedBuiltinTemplateId}`
                      : selectedTemplateAssetId != null
                        ? `asset:${selectedTemplateAssetId}`
                        : ''
                  }
                  onChange={(event) => {
                    const value = event.target.value;
                    if (!value) {
                      setSelectedBuiltinTemplateId('');
                      setSelectedTemplateAssetId(null);
                      return;
                    }
                    if (value.startsWith('builtin:')) {
                      setSelectedBuiltinTemplateId(value.slice('builtin:'.length));
                      setSelectedTemplateAssetId(null);
                      return;
                    }
                    if (value.startsWith('asset:')) {
                      setSelectedBuiltinTemplateId('');
                      setSelectedTemplateAssetId(Number(value.slice('asset:'.length)));
                    }
                  }}
                  className={inputClass}
                >
                  <option value="">不使用模板 PDF</option>
                  {builtinTemplates.length > 0 && (
                    <optgroup label="内置模板">
                      {builtinTemplates.map(item => (
                        <option key={item.id} value={`builtin:${item.id}`}>
                          {item.name}（内置）
                        </option>
                      ))}
                    </optgroup>
                  )}
                  {templateAssets.length > 0 && (
                    <optgroup label="我上传的模板">
                      {templateAssets.map(item => (
                        <option key={item.id} value={`asset:${item.id}`}>
                          {item.originalFilename}
                        </option>
                      ))}
                    </optgroup>
                  )}
                </select>
              </label>
            </div>

            <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto_auto]">
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">指定模型（可选）</span>
                <select value={providerId} onChange={(event) => setProviderId(event.target.value)} className={inputClass}>
                  <option value="">使用当前账号默认模型</option>
                  {providerOptions.map(option => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
              <Link
                to="/upload"
                className="inline-flex items-center justify-center gap-2 self-end rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:border-primary-300 hover:text-primary-600 dark:border-slate-700 dark:text-slate-200"
              >
                <Upload className="h-4 w-4" />
                去上传简历
              </Link>
              {selectedResumeId ? (
                <Link
                  to={`/resumes/${selectedResumeId}`}
                  className="inline-flex items-center justify-center gap-2 self-end rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:border-primary-300 hover:text-primary-600 dark:border-slate-700 dark:text-slate-200"
                >
                  <FileText className="h-4 w-4" />
                  查看简历详情
                </Link>
              ) : (
                <span />
              )}
            </div>

            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-white">模板 PDF</h3>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    下拉框里已经包含内置模板，也支持上传你自己的模板 PDF，生成时会和解析后的简历文本一起发给模型。
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    ref={templateInputRef}
                    type="file"
                    accept="application/pdf,.pdf"
                    className="hidden"
                    onChange={(event) => void handleTemplateUpload(event.target.files?.[0])}
                  />
                  <button
                    type="button"
                    onClick={() => templateInputRef.current?.click()}
                    disabled={uploadingTemplate}
                    className="btn-secondary flex items-center gap-2 rounded-xl px-4 py-2 text-sm"
                  >
                    {uploadingTemplate ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
                    上传模板 PDF
                  </button>
                </div>
              </div>

              {templateAssets.length > 0 ? (
                <div className="mt-4 space-y-2">
                  {templateAssets.map((item) => {
                    const isSelected = item.id === selectedTemplateAssetId;
                    return (
                      <div
                        key={item.id}
                        className={`flex items-center justify-between gap-3 rounded-xl border px-3 py-2.5 text-sm ${
                          isSelected
                            ? 'border-primary-300 bg-primary-50/70 dark:border-primary-700 dark:bg-primary-950/30'
                            : 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-950/20'
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedBuiltinTemplateId('');
                            setSelectedTemplateAssetId(item.id);
                          }}
                          className="min-w-0 flex-1 text-left"
                        >
                          <div className="truncate font-medium text-slate-900 dark:text-slate-100">{item.originalFilename}</div>
                          <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                            {(item.fileSize / 1024 / 1024).toFixed(2)} MB
                          </div>
                        </button>
                        <button
                          type="button"
                          onClick={() => void handleDeleteTemplate(item.id)}
                          disabled={deletingTemplateId === item.id}
                          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-rose-300 hover:text-rose-600 dark:border-slate-700 dark:text-slate-300"
                          title="删除模板"
                        >
                          {deletingTemplateId === item.id
                            ? <Loader2 className="h-4 w-4 animate-spin" />
                            : <Trash2 className="h-4 w-4" />}
                        </button>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
                  还没有上传模板 PDF，当前会按纯文本方式生成。
                </div>
              )}
            </div>

            <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <button
                type="button"
                onClick={() => setBuiltinTemplatesExpanded((prev) => !prev)}
                className="flex w-full items-center justify-between gap-3 text-left"
              >
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-white">内置模板参考</h3>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    这里已经内置了 `resume-master` 的 5 套参考模板，需要时再展开查看。
                  </p>
                </div>
                <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-500 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-300">
                  <ChevronDown
                    className={`h-4 w-4 transition-transform ${builtinTemplatesExpanded ? 'rotate-180' : ''}`}
                  />
                </span>
              </button>

              {builtinTemplatesExpanded && (
                <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {builtinTemplates.map((template) => (
                    <article
                      key={template.id}
                      className={`overflow-hidden rounded-xl border bg-white dark:bg-slate-950/20 ${
                        selectedBuiltinTemplateId === template.id
                          ? 'border-primary-400 ring-2 ring-primary-100 dark:border-primary-600 dark:ring-primary-900/30'
                          : 'border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      <div className="aspect-[3/4] overflow-hidden bg-slate-100 dark:bg-slate-900">
                        <img
                          src={toBackendAssetUrl(template.previewImageUrl)}
                          alt={template.name}
                          className="h-full w-full object-cover object-top"
                        />
                      </div>
                      <div className="space-y-3 p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <h4 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{template.name}</h4>
                            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{template.description}</p>
                          </div>
                          <span
                            className="inline-flex rounded-full px-2.5 py-1 text-[11px] font-medium"
                            style={{ backgroundColor: `${template.accentColor}18`, color: template.accentColor }}
                          >
                            {template.recommendedScenario}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedBuiltinTemplateId(template.id);
                              setSelectedTemplateAssetId(null);
                            }}
                            className="inline-flex items-center gap-1.5 rounded-lg border border-primary-200 px-3 py-1.5 text-xs font-medium text-primary-700 transition hover:border-primary-400 dark:border-primary-700 dark:text-primary-300"
                          >
                            选用此模板
                          </button>
                          <a
                            href={toBackendAssetUrl(template.referencePdfUrl)}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-primary-300 hover:text-primary-600 dark:border-slate-700 dark:text-slate-200"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                            查看 PDF
                          </a>
                          <a
                            href={toBackendAssetUrl(template.referenceHtmlUrl)}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-primary-300 hover:text-primary-600 dark:border-slate-700 dark:text-slate-200"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                            查看 HTML
                          </a>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <TextAreaField
              label="已上传简历解析文本"
              value={resumePreviewText}
              onChange={() => undefined}
              rows={18}
              readOnly
              placeholder="选择一份已上传简历后，这里会显示系统解析出的文本内容。"
            />

            <TextAreaField
              label="JD 文本"
              value={jdText}
              onChange={setJdText}
              rows={12}
              placeholder="可选。留空时，大模型会直接根据简历原文完成优化与排版。"
            />

            <TextAreaField
              label="其他描述"
              value={extraNotes}
              onChange={setExtraNotes}
              rows={8}
              placeholder="可选。这里可以补充你希望模型特别注意的点，比如强调项目、压缩篇幅、突出某类能力、避免某种表达风格等。"
            />
          </div>
          </>
          ) : (
            <div className="space-y-4">
              <div>
                <h2 className="text-sm font-semibold text-slate-900 dark:text-white">已生成简历</h2>
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  可以回看之前生成过的 HTML 简历，也可以删除不需要的记录。
                </p>
              </div>

              {generatedResumes.length > 0 ? (
                <div className="space-y-3">
                  {generatedResumes.map((item) => {
                    const isSelected = item.id === selectedGeneratedResumeId;
                    return (
                      <div
                        key={item.id}
                        className={`rounded-2xl border px-4 py-3 ${
                          isSelected
                            ? 'border-primary-300 bg-primary-50/60 dark:border-primary-700 dark:bg-primary-950/20'
                            : 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900/40'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <button
                            type="button"
                            onClick={() => void loadGeneratedResume(item.id)}
                            className="min-w-0 flex-1 text-left"
                          >
                            <div className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">
                              {item.title}
                            </div>
                            <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                              来源：{item.sourceResumeName}
                              {item.targetRole ? ` · 目标岗位：${item.targetRole}` : ''}
                              {item.targetPageCount ? ` · ${item.targetPageCount}页` : ''}
                            </div>
                            <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                              更新时间：{new Date(item.updatedAt).toLocaleString()}
                            </div>
                          </button>
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => void loadGeneratedResume(item.id)}
                              disabled={loadingGeneratedResumeId === item.id}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-primary-300 hover:text-primary-600 dark:border-slate-700 dark:text-slate-300"
                              title="查看简历"
                            >
                              {loadingGeneratedResumeId === item.id
                                ? <Loader2 className="h-4 w-4 animate-spin" />
                                : <FileText className="h-4 w-4" />}
                            </button>
                            <button
                              type="button"
                              onClick={() => setGeneratedResumeToDelete(item)}
                              disabled={deletingGeneratedResumeId === item.id}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-rose-300 hover:text-rose-600 dark:border-slate-700 dark:text-slate-300"
                              title="删除记录"
                            >
                              {deletingGeneratedResumeId === item.id
                                ? <Loader2 className="h-4 w-4 animate-spin" />
                                : <Trash2 className="h-4 w-4" />}
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="flex min-h-[420px] items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 text-center dark:border-slate-700 dark:bg-slate-900/40">
                  <div className="max-w-sm space-y-3 px-6">
                    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-50 text-primary-500 dark:bg-primary-900/20 dark:text-primary-300">
                      <FolderOpen className="h-6 w-6" />
                    </div>
                    <h3 className="text-base font-semibold text-slate-900 dark:text-white">还没有生成记录</h3>
                    <p className="text-sm text-slate-500 dark:text-slate-400">
                      先在“生成配置”里生成一份简历，这里就会自动保存并展示。
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}
          </div>
        </section>

        <section className={`${sectionCardClass} ${workspacePanelClass}`}>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-900 dark:text-white">在线预览</h2>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                大模型直接输出完整 HTML 简历；如果选择了模板 PDF，也会一并作为参考输入。
              </p>
            </div>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500 dark:bg-slate-800 dark:text-slate-300">
              {generatedResult ? '已生成' : '待生成'}
            </span>
          </div>

          {htmlPreview ? (
            <div className={previewViewportClass}>
              <iframe
                ref={previewFrameRef}
                title="resume-preview"
                srcDoc={htmlPreview}
                onLoad={handlePreviewFrameLoad}
                className="h-[980px] w-full bg-white"
                sandbox="allow-same-origin allow-modals"
              />
            </div>
          ) : generating ? (
            <div className="flex h-[980px] shrink-0 items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 text-center dark:border-slate-700 dark:bg-slate-900/40">
              <div className="max-w-sm space-y-4 px-6">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-500 dark:bg-primary-900/20 dark:text-primary-300">
                  <Loader2 className="h-7 w-7 animate-spin" />
                </div>
                <div className="space-y-2">
                  <h3 className="text-base font-semibold text-slate-900 dark:text-white">正在生成简历</h3>
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    正在调用大模型整理内容、优化表达并生成 HTML 预览，请稍等片刻。
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex h-[980px] shrink-0 items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 text-center dark:border-slate-700 dark:bg-slate-900/40">
              <div className="max-w-sm space-y-3 px-6">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-50 text-primary-500 dark:bg-primary-900/20 dark:text-primary-300">
                  <Sparkles className="h-6 w-6" />
                </div>
                <h3 className="text-base font-semibold text-slate-900 dark:text-white">预览区已就绪</h3>
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  先选择一份已上传简历，然后点击“生成简历”。
                </p>
              </div>
            </div>
          )}
        </section>
      </div>

      <DeleteConfirmDialog
        open={generatedResumeToDelete != null}
        item={generatedResumeToDelete}
        itemType="已生成简历"
        loading={generatedResumeToDelete != null && deletingGeneratedResumeId === generatedResumeToDelete.id}
        onConfirm={() => {
          if (generatedResumeToDelete) {
            void handleDeleteGeneratedResume(generatedResumeToDelete.id);
          }
        }}
        onCancel={() => {
          if (deletingGeneratedResumeId == null) {
            setGeneratedResumeToDelete(null);
          }
        }}
      />
    </div>
  );
}

function applyPrintStyles(doc: Document) {
  const styleId = 'resume-maker-print-style';
  let styleEl = doc.getElementById(styleId) as HTMLStyleElement | null;
  if (!styleEl) {
    styleEl = doc.createElement('style');
    styleEl.id = styleId;
    doc.head.appendChild(styleEl);
  }
  styleEl.textContent = `
    @page {
      size: A4;
      margin: 6mm 6mm 8mm 6mm;
    }
    @media print {
      html, body {
        margin: 0 !important;
        padding: 0 !important;
        background: #fff !important;
        width: auto !important;
        min-height: auto !important;
        height: auto !important;
        overflow: visible !important;
      }

      body > *:first-child,
      .resume,
      .resume-container,
      .page,
      .container,
      main,
      article,
      section {
        margin-top: 0 !important;
      }

      body,
      .resume,
      .resume-container,
      .container,
      main {
        min-height: auto !important;
        height: auto !important;
        max-height: none !important;
        overflow: visible !important;
        padding-top: 0 !important;
        padding-bottom: 0 !important;
      }

      .resume,
      .resume-container,
      .container {
        padding-left: 0 !important;
        padding-right: 0 !important;
      }

      .left,
      .right,
      .sidebar,
      .main,
      .content,
      aside,
      section,
      header {
        margin-top: 0 !important;
        margin-bottom: 0 !important;
      }

      .resume {
        zoom: 1 !important;
        transform: none !important;
      }

      .page {
        width: auto !important;
        min-width: 0 !important;
        max-width: none !important;
        height: auto !important;
        min-height: 0 !important;
        max-height: none !important;
        margin: 0 !important;
        padding: 0 !important;
        overflow: visible !important;
        box-sizing: border-box !important;
        break-after: auto !important;
        page-break-after: auto !important;
      }

      .left,
      .right,
      .sidebar,
      .main,
      .content {
        overflow: visible !important;
      }

      section,
      article,
      .section,
      .entry,
      .item,
      li {
        break-inside: auto !important;
        page-break-inside: auto !important;
      }

      h1,
      h2,
      h3,
      h4,
      .section-title,
      .entry-header {
        break-after: avoid !important;
        page-break-after: avoid !important;
      }

      ul,
      ol {
        break-inside: auto !important;
        page-break-inside: auto !important;
      }
    }
  `;
}

async function printHtmlPreview(html: string): Promise<void> {
  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  iframe.style.opacity = '0';
  iframe.setAttribute('aria-hidden', 'true');
  document.body.appendChild(iframe);

  try {
    await new Promise<void>((resolve, reject) => {
      iframe.onload = () => resolve();
      iframe.onerror = () => reject(new Error('打印预览加载失败'));
      iframe.srcdoc = html;
    });

    const printWindow = iframe.contentWindow;
    const printDocument = iframe.contentDocument;
    if (!printWindow || !printDocument) {
      throw new Error('打印预览尚未准备完成，请稍后重试');
    }

    applyPrintStyles(printDocument);

    await new Promise<void>((resolve) => {
      let done = false;
      const finish = () => {
        if (done) {
          return;
        }
        done = true;
        printWindow.removeEventListener('afterprint', finish);
        mediaQuery?.removeEventListener?.('change', handlePrintMediaChange);
        resolve();
      };
      const handlePrintMediaChange = (event: MediaQueryListEvent) => {
        if (!event.matches) {
          finish();
        }
      };

      const mediaQuery = printWindow.matchMedia?.('print');
      printWindow.addEventListener('afterprint', finish, { once: true });
      mediaQuery?.addEventListener?.('change', handlePrintMediaChange);

      printWindow.focus();
      printWindow.print();
      window.setTimeout(finish, 2000);
    });
  } finally {
    iframe.remove();
  }
}

function applyPreviewStyles(doc: Document) {
  const styleId = 'resume-maker-preview-style';
  let styleEl = doc.getElementById(styleId) as HTMLStyleElement | null;
  if (!styleEl) {
    styleEl = doc.createElement('style');
    styleEl.id = styleId;
    doc.head.appendChild(styleEl);
  }

  styleEl.textContent = `
    html, body {
      margin: 0 !important;
      padding: 0 !important;
      background: #ffffff !important;
      overflow-x: hidden !important;
      overflow-y: auto !important;
    }

    body {
      padding: 16px 16px 24px !important;
      box-sizing: border-box !important;
    }

    body > *:first-child,
    .resume,
    .resume-container,
    .page,
    .container,
    main {
      margin-top: 0 !important;
    }

  `;
}

function normalizePreviewTopOverflow(win: Window, doc: Document) {
  const root = findResumeRoot(doc);
  if (!root) {
    return;
  }

  root.style.marginTop = '0px';
  root.style.transform = '';

  const elements = Array.from(doc.body.querySelectorAll<HTMLElement>('*'));
  if (elements.length === 0) {
    return;
  }

  let minTop = 0;
  for (const element of elements) {
    const rect = element.getBoundingClientRect();
    if (Number.isFinite(rect.top)) {
      minTop = Math.min(minTop, rect.top);
    }
  }

  if (minTop < 0) {
    root.style.marginTop = `${Math.ceil(Math.abs(minTop)) + 8}px`;
    win.scrollTo(0, 0);
  }
}

function applyTemplateSpecificPreviewAdjustments(doc: Document, templateId?: string) {
  const root = findResumeRoot(doc);
  if (!root || !templateId) {
    return;
  }

  switch (templateId) {
    case 'calm-dual': {
      root.style.overflow = 'visible';
      const leftColumn = root.querySelector<HTMLElement>('.left');
      if (leftColumn) {
        leftColumn.style.overflow = 'visible';
      }
      break;
    }
    case 'elegant-red': {
      const photoWrapper = doc.querySelector<HTMLElement>('.photo-wrapper');
      if (photoWrapper) {
        photoWrapper.style.top = '0px';
      }
      break;
    }
    case 'fresh-blue-gray': {
      const avatar = doc.querySelector<HTMLElement>('.avatar');
      if (avatar) {
        avatar.style.top = '0px';
      }
      break;
    }
    default:
      break;
  }
}

function resetPreviewScrollPosition(win: Window, doc: Document) {
  win.scrollTo(0, 0);
  doc.documentElement.scrollTop = 0;
  doc.body.scrollTop = 0;
}

function fitPreviewToViewport(win: Window, doc: Document) {
  const root = findResumeRoot(doc);
  if (!root) {
    return;
  }
  const pageCount = doc.querySelectorAll('.page').length;

  doc.body.style.zoom = '';
  root.style.transform = '';
  root.style.transformOrigin = 'top left';
  root.style.marginLeft = '0';
  root.style.marginRight = '0';
  root.style.width = '';
  root.style.maxWidth = '';

  const viewportWidth = Math.max(doc.documentElement.clientWidth, win.innerWidth || 0);
  const rootWidth = Math.max(root.scrollWidth, root.getBoundingClientRect().width);
  if (!viewportWidth || !rootWidth) {
    return;
  }

  const scale = Math.min(1, viewportWidth / rootWidth);
  if (pageCount > 1) {
    doc.body.style.zoom = String(scale);
    doc.body.style.transformOrigin = 'top left';
    doc.body.style.minHeight = '';
    root.style.width = '';
    return;
  }

  root.style.transform = `scale(${scale})`;
  root.style.transformOrigin = 'top left';
  root.style.width = `${rootWidth}px`;
  doc.body.style.minHeight = `${Math.ceil(root.scrollHeight * scale)}px`;
}

function findResumeRoot(doc: Document): HTMLElement | null {
  return doc.querySelector<HTMLElement>('.resume-pages, .resume, .resume-container, .container, main, .page')
    ?? doc.body.firstElementChild as HTMLElement | null;
}

function normalizeTargetPageCount(value: string): number {
  const parsed = Number.parseInt(value.trim(), 10);
  if (!Number.isFinite(parsed)) {
    return 1;
  }
  return Math.max(1, Math.min(10, parsed));
}

function InputField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} className={inputClass} />
    </label>
  );
}

function TextAreaField({
  label,
  value,
  onChange,
  rows,
  placeholder,
  readOnly = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  rows: number;
  placeholder?: string;
  readOnly?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</span>
      <textarea
        rows={rows}
        value={value}
        readOnly={readOnly}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className={`${textareaClass} min-h-[120px] ${readOnly ? 'bg-slate-50 text-slate-500 dark:bg-slate-900/60 dark:text-slate-300' : ''}`}
      />
    </label>
  );
}
