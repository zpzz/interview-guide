import { useState } from 'react';
import { ArrowRight, Eye, EyeOff, LockKeyhole, Sparkles, UserRound } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import request, { getErrorMessage, setAuthToken } from '../api/request';
import loginBackground from '../assets/login-background.png';
import loginCardShell from '../assets/login-card-shell.png';

interface AuthResponse {
  token: string;
  user: {
    id: number;
    username: string;
    nickname: string;
    roles: string[];
  };
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    if (!username.trim() || !password) {
      setError('请输入账号和密码');
      return;
    }

    try {
      setSubmitting(true);
      const response = await request.post<AuthResponse>('/api/auth/login', {
        username: username.trim(),
        password,
      });
      setAuthToken(response.token, rememberMe);
      navigate('/history', { replace: true });
    } catch (loginError) {
      setError(getErrorMessage(loginError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 px-5 py-8 text-white">
      <img
        src={loginBackground}
        alt=""
        aria-hidden="true"
        className="absolute inset-0 h-full w-full object-cover"
      />
      <div className="absolute inset-0 bg-slate-950/20" />

      <section className="relative z-10 aspect-[1149/1369] w-[min(92vw,640px,82vh)]">
        <img
          src={loginCardShell}
          alt=""
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 h-full w-full select-none"
        />

        <div className="absolute inset-x-[13%] inset-y-[9%] flex flex-col justify-center">
          <div className="mb-10 flex items-center justify-between gap-4">
            <div>
              <p className="text-base text-slate-300/85">欢迎回来</p>
              <h1 className="mt-2 text-[28px] font-semibold leading-tight text-white">
                登录 Interview Guide
              </h1>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-cyan-300/35 bg-cyan-300/10 text-cyan-100 shadow-lg shadow-cyan-500/20">
              <Sparkles className="h-6 w-6" />
            </div>
          </div>

          <form className="space-y-6" onSubmit={handleSubmit}>
            <label className="block">
              <span className="mb-2.5 block text-base font-medium text-slate-100">账号</span>
              <div className="relative">
                <UserRound className="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300/80" />
                <input
                  type="text"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="请输入用户名 / 邮箱 / 手机号"
                  autoComplete="username"
                  className="h-[58px] w-full rounded-2xl border border-white/15 bg-white/10 px-14 text-base text-white outline-none transition placeholder:text-slate-300/65 focus:border-cyan-300/70 focus:bg-white/15 focus:ring-4 focus:ring-cyan-300/12"
                />
              </div>
            </label>

            <label className="block">
              <span className="mb-2.5 block text-base font-medium text-slate-100">密码</span>
              <div className="relative">
                <LockKeyhole className="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300/80" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="请输入密码"
                  autoComplete="current-password"
                  className="h-[58px] w-full rounded-2xl border border-white/15 bg-white/10 px-14 pr-16 text-base text-white outline-none transition placeholder:text-slate-300/65 focus:border-cyan-300/70 focus:bg-white/15 focus:ring-4 focus:ring-cyan-300/12"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  className="absolute inset-y-0 right-0 flex items-center px-5 text-slate-300 transition hover:text-white"
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </label>

            <div className="flex items-center justify-between gap-4 text-base">
              <label className="inline-flex items-center gap-2 text-slate-200">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => setRememberMe(event.target.checked)}
                  className="h-5 w-5 rounded border-white/30 bg-white/10 text-cyan-500 focus:ring-cyan-300"
                />
                记住我
              </label>
              <button type="button" className="text-cyan-200 transition hover:text-white">
                忘记密码？
              </button>
            </div>

            {error && (
              <div className="rounded-xl border border-rose-300/30 bg-rose-500/15 px-4 py-3 text-sm text-rose-100">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="group mt-2 flex h-16 w-full items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-cyan-400 via-sky-400 to-fuchsia-400 px-4 text-xl font-semibold text-white shadow-xl shadow-cyan-500/25 transition hover:-translate-y-0.5 hover:shadow-cyan-500/35 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
            >
              {submitting ? '登录中...' : '进入系统'}
              <ArrowRight className="h-5 w-5 transition group-hover:translate-x-0.5" />
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
