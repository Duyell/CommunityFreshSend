import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as authApi from '../api/auth';

export default function Register() {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!phone || !password || !nickname) {
      setError('请填写所有字段');
      return;
    }
    if (phone.length !== 11) {
      setError('请输入正确的11位手机号');
      return;
    }
    if (password.length < 6) {
      setError('密码至少6位');
      return;
    }
    setLoading(true);
    try {
      const user = await authApi.register(phone, password, nickname);
      localStorage.setItem('token', user.token);
      localStorage.setItem('user', JSON.stringify(user));
      navigate('/');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { msg?: string } } })?.response?.data?.msg;
      setError(msg || '注册失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <span className="text-5xl">🥬</span>
          <h1 className="text-2xl font-bold text-gray-800 mt-3">创建账号</h1>
          <p className="text-gray-500 mt-1">加入社区生鲜配送</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4">
          {error && (
            <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">{error}</div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">昵称</label>
            <input
              type="text"
              className="input"
              placeholder="请输入昵称"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">手机号</label>
            <input
              type="tel"
              className="input"
              placeholder="请输入11位手机号"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              maxLength={11}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">密码</label>
            <input
              type="password"
              className="input"
              placeholder="至少6位密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? '注册中...' : '注册'}
          </button>
          <p className="text-center text-sm text-gray-500">
            已有账号？<Link to="/login" className="text-emerald-600 font-medium hover:underline">去登录</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
