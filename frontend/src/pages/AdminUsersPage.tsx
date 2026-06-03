import {FormEvent, useEffect, useMemo, useState} from 'react';
import {
  CheckCircle,
  Edit2,
  KeyRound,
  Loader2,
  Plus,
  Search,
  Shield,
  Trash2,
  UserCog,
  X,
  XCircle,
} from 'lucide-react';
import {adminUserApi} from '../api/adminUser';
import {getCurrentUserId, getErrorMessage} from '../api/request';
import type {AdminUser} from '../types/adminUser';

const ROLE_OPTIONS = [
  {value: 'USER', label: '普通用户'},
  {value: 'ADMIN', label: '管理员'},
];

const CARD_CLASS = 'rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800';

export default function AdminUsersPage() {
  const currentUserId = getCurrentUserId();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [enabledFilter, setEnabledFilter] = useState('');
  const [toast, setToast] = useState<{message: string; type: 'success' | 'error'} | null>(null);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [showUserModal, setShowUserModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState<AdminUser | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null);
  const [saving, setSaving] = useState(false);

  const [formUsername, setFormUsername] = useState('');
  const [formPassword, setFormPassword] = useState('');
  const [formNickname, setFormNickname] = useState('');
  const [formEnabled, setFormEnabled] = useState(true);
  const [formRole, setFormRole] = useState('USER');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const filteredParams = useMemo(() => ({
    keyword: keyword.trim() || undefined,
    role: roleFilter || undefined,
    enabled: enabledFilter === '' ? undefined : enabledFilter === 'true',
  }), [enabledFilter, keyword, roleFilter]);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({message, type});
    window.setTimeout(() => setToast(null), 3000);
  };

  const loadUsers = async () => {
    setLoading(true);
    try {
      setUsers(await adminUserApi.list(filteredParams));
    } catch (error) {
      showToast(getErrorMessage(error), 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = (event: FormEvent) => {
    event.preventDefault();
    loadUsers();
  };

  const openCreateModal = () => {
    setEditingUser(null);
    setFormUsername('');
    setFormPassword('');
    setFormNickname('');
    setFormEnabled(true);
    setFormRole('USER');
    setShowUserModal(true);
  };

  const openEditModal = (user: AdminUser) => {
    setEditingUser(user);
    setFormUsername(user.username);
    setFormPassword('');
    setFormNickname(user.nickname ?? '');
    setFormEnabled(user.enabled);
    setFormRole(user.roles.includes('ADMIN') ? 'ADMIN' : 'USER');
    setShowUserModal(true);
  };

  const closeUserModal = () => {
    if (!saving) {
      setShowUserModal(false);
      setEditingUser(null);
    }
  };

  const handleSubmitUser = async (event: FormEvent) => {
    event.preventDefault();
    if (!editingUser && (!formUsername.trim() || !formPassword.trim())) {
      showToast('请填写账号和初始密码', 'error');
      return;
    }
    setSaving(true);
    try {
      if (editingUser) {
        await adminUserApi.update(editingUser.id, {
          nickname: formNickname.trim() || undefined,
          enabled: formEnabled,
          roles: [formRole],
        });
        showToast('用户已更新');
      } else {
        await adminUserApi.create({
          username: formUsername.trim(),
          password: formPassword.trim(),
          nickname: formNickname.trim() || undefined,
          enabled: formEnabled,
          roles: [formRole],
        });
        showToast('用户已创建');
      }
      closeUserModal();
      await loadUsers();
    } catch (error) {
      showToast(getErrorMessage(error), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleResetPassword = async (event: FormEvent) => {
    event.preventDefault();
    if (!showPasswordModal) {
      return;
    }
    if (!password.trim() || password !== confirmPassword) {
      showToast('请确认两次密码一致', 'error');
      return;
    }
    setSaving(true);
    try {
      await adminUserApi.resetPassword(showPasswordModal.id, {password: password.trim()});
      showToast('密码已重置');
      setShowPasswordModal(null);
      setPassword('');
      setConfirmPassword('');
    } catch (error) {
      showToast(getErrorMessage(error), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) {
      return;
    }
    setSaving(true);
    try {
      await adminUserApi.delete(deleteTarget.id);
      showToast('用户已删除');
      setDeleteTarget(null);
      await loadUsers();
    } catch (error) {
      showToast(getErrorMessage(error), 'error');
    } finally {
      setSaving(false);
    }
  };

  const isSelf = (user: AdminUser) => user.id === currentUserId;
  const actionDisabled = (user: AdminUser) => user.protectedAccount;

  return (
    <div className="mx-auto max-w-7xl">
      {toast && (
        <div className={`fixed right-6 top-6 z-50 rounded-xl px-4 py-3 text-sm font-medium shadow-lg ${
          toast.type === 'success'
            ? 'bg-emerald-500 text-white'
            : 'bg-red-500 text-white'
        }`}>
          {toast.message}
        </div>
      )}

      <div className="mb-8 flex items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="rounded-2xl bg-gradient-to-r from-primary-500 to-primary-600 p-3 text-white shadow-lg shadow-primary-500/25">
            <UserCog className="h-7 w-7" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">用户管理</h1>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">管理系统账号、角色和启用状态</p>
          </div>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-2 rounded-xl bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary-500/20 transition hover:bg-primary-600"
        >
          <Plus className="h-4 w-4" />
          新增用户
        </button>
      </div>

      <form onSubmit={handleSearch} className={`${CARD_CLASS} mb-5 p-4`}>
        <div className="grid gap-3 md:grid-cols-[1fr_160px_160px_auto]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索账号或昵称"
              className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:border-primary-400 focus:ring-4 focus:ring-primary-500/10 dark:border-slate-700 dark:bg-slate-900 dark:text-white"
            />
          </div>
          <select
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value)}
            className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
          >
            <option value="">全部角色</option>
            {ROLE_OPTIONS.map((role) => (
              <option key={role.value} value={role.value}>{role.label}</option>
            ))}
          </select>
          <select
            value={enabledFilter}
            onChange={(event) => setEnabledFilter(event.target.value)}
            className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
          >
            <option value="">全部状态</option>
            <option value="true">已启用</option>
            <option value="false">已禁用</option>
          </select>
          <button className="h-11 rounded-xl bg-slate-900 px-5 text-sm font-semibold text-white transition hover:bg-slate-700 dark:bg-white dark:text-slate-900">
            查询
          </button>
        </div>
      </form>

      <div className={CARD_CLASS}>
        {loading ? (
          <div className="flex items-center justify-center py-24">
            <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase text-slate-500 dark:border-slate-700 dark:text-slate-400">
                <tr>
                  <th className="px-5 py-4">账号</th>
                  <th className="px-5 py-4">昵称</th>
                  <th className="px-5 py-4">角色</th>
                  <th className="px-5 py-4">状态</th>
                  <th className="px-5 py-4">创建时间</th>
                  <th className="px-5 py-4 text-right">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {users.map((user) => (
                  <tr key={user.id} className="text-slate-700 dark:text-slate-200">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold">{user.username}</span>
                        {user.protectedAccount && (
                          <span title="内置保护账号" className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
                            保护
                          </span>
                        )}
                        {isSelf(user) && (
                          <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                            当前
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-4">{user.nickname || '-'}</td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-1.5">
                        {user.roles.map((role) => (
                          <span key={role} className="inline-flex items-center gap-1 rounded-full bg-primary-50 px-2 py-1 text-xs font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
                            <Shield className="h-3 w-3" />
                            {role}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${
                        user.enabled
                          ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                          : 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-300'
                      }`}>
                        {user.enabled ? <CheckCircle className="h-3 w-3" /> : <XCircle className="h-3 w-3" />}
                        {user.enabled ? '启用' : '禁用'}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-slate-500 dark:text-slate-400">
                      {new Date(user.createdAt).toLocaleString()}
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => openEditModal(user)}
                          disabled={actionDisabled(user)}
                          className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-white"
                          title="编辑"
                        >
                          <Edit2 className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setShowPasswordModal(user)}
                          disabled={actionDisabled(user)}
                          className="rounded-lg p-2 text-blue-500 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-blue-900/20"
                          title="重置密码"
                        >
                          <KeyRound className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeleteTarget(user)}
                          disabled={actionDisabled(user) || isSelf(user)}
                          className="rounded-lg p-2 text-red-500 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-red-900/20"
                          title="删除"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {users.length === 0 && (
              <div className="py-16 text-center text-sm text-slate-500 dark:text-slate-400">
                暂无用户数据
              </div>
            )}
          </div>
        )}
      </div>

      {showUserModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleSubmitUser} className={`${CARD_CLASS} w-full max-w-lg p-6`}>
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                {editingUser ? '编辑用户' : '新增用户'}
              </h2>
              <button type="button" onClick={closeUserModal} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-700">
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="space-y-4">
              <label className="block">
                <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">账号</span>
                <input
                  value={formUsername}
                  onChange={(event) => setFormUsername(event.target.value)}
                  disabled={!!editingUser}
                  className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none disabled:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-white dark:disabled:bg-slate-800"
                />
              </label>
              {!editingUser && (
                <label className="block">
                  <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">初始密码</span>
                  <input
                    type="password"
                    value={formPassword}
                    onChange={(event) => setFormPassword(event.target.value)}
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                  />
                </label>
              )}
              <label className="block">
                <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">昵称</span>
                <input
                  value={formNickname}
                  onChange={(event) => setFormNickname(event.target.value)}
                  className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
                />
              </label>
              <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-200">
                <input
                  type="checkbox"
                  checked={formEnabled}
                  onChange={(event) => setFormEnabled(event.target.checked)}
                  className="h-4 w-4 rounded border-slate-300 text-primary-500"
                />
                启用账号
              </label>
              <div>
                <span className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-200">角色</span>
                <div className="flex gap-2">
                  {ROLE_OPTIONS.map((role) => (
                    <button
                      key={role.value}
                      type="button"
                      onClick={() => setFormRole(role.value)}
                      aria-pressed={formRole === role.value}
                      className={`rounded-xl border px-3 py-2 text-sm font-medium transition ${
                        formRole === role.value
                          ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-700'
                      }`}
                    >
                      {role.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={closeUserModal} className="rounded-xl px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700">
                取消
              </button>
              <button disabled={saving} className="rounded-xl bg-primary-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}

      {showPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleResetPassword} className={`${CARD_CLASS} w-full max-w-md p-6`}>
            <h2 className="mb-5 text-lg font-bold text-slate-900 dark:text-white">
              重置密码：{showPasswordModal.username}
            </h2>
            <div className="space-y-4">
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="新密码"
                className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
              />
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="确认新密码"
                className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none dark:border-slate-700 dark:bg-slate-900 dark:text-white"
              />
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setShowPasswordModal(null)} className="rounded-xl px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700">
                取消
              </button>
              <button disabled={saving} className="rounded-xl bg-primary-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
                确认重置
              </button>
            </div>
          </form>
        </div>
      )}

      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className={`${CARD_CLASS} w-full max-w-md p-6`}>
            <h2 className="text-lg font-bold text-slate-900 dark:text-white">删除用户</h2>
            <p className="mt-3 text-sm leading-6 text-slate-500 dark:text-slate-400">
              确定要删除用户 “{deleteTarget.username}” 吗？删除后该账号将无法登录，但历史业务数据会保留。
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setDeleteTarget(null)} className="rounded-xl px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700">
                取消
              </button>
              <button onClick={handleDelete} disabled={saving} className="rounded-xl bg-red-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
                确认删除
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
