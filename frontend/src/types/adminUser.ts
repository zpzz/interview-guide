export interface AdminUser {
  id: number;
  username: string;
  nickname: string | null;
  enabled: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
  protectedAccount: boolean;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  nickname?: string;
  enabled: boolean;
  roles: string[];
}

export interface UpdateUserRequest {
  nickname?: string;
  enabled: boolean;
  roles: string[];
}

export interface ResetPasswordRequest {
  password: string;
}
