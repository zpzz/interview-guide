import request from './request';
import type {
  AdminUser,
  CreateUserRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
} from '../types/adminUser';

export const adminUserApi = {
  list(params?: {
    keyword?: string;
    role?: string;
    enabled?: boolean;
  }): Promise<AdminUser[]> {
    const search = new URLSearchParams();
    if (params?.keyword) {
      search.set('keyword', params.keyword);
    }
    if (params?.role) {
      search.set('role', params.role);
    }
    if (params?.enabled !== undefined) {
      search.set('enabled', String(params.enabled));
    }
    const query = search.toString();
    return request.get<AdminUser[]>(`/api/admin/users${query ? `?${query}` : ''}`);
  },

  create(data: CreateUserRequest): Promise<AdminUser> {
    return request.post<AdminUser>('/api/admin/users', data);
  },

  update(id: number, data: UpdateUserRequest): Promise<AdminUser> {
    return request.put<AdminUser>(`/api/admin/users/${id}`, data);
  },

  resetPassword(id: number, data: ResetPasswordRequest): Promise<void> {
    return request.put<void>(`/api/admin/users/${id}/password`, data);
  },

  delete(id: number): Promise<void> {
    return request.delete<void>(`/api/admin/users/${id}`);
  },
};
