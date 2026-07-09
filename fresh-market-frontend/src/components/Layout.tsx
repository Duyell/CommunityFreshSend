import { useState } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { ShoppingCartIcon, HomeIcon, ClipboardDocumentListIcon } from '@heroicons/react/24/outline';

export default function Layout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const userStr = localStorage.getItem('user');
  const user = userStr ? JSON.parse(userStr) : null;

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-gray-100">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 text-xl font-bold text-emerald-700">
            <span className="text-2xl">🥬</span>
            <span>鲜送达</span>
          </Link>

          {/* 导航链接 */}
          <nav className="hidden md:flex items-center gap-6">
            <Link to="/" className="flex items-center gap-1.5 text-gray-600 hover:text-emerald-600 transition-colors">
              <HomeIcon className="w-5 h-5" />
              <span>首页</span>
            </Link>
            {token && (
              <>
                <Link to="/cart" className="flex items-center gap-1.5 text-gray-600 hover:text-emerald-600 transition-colors">
                  <ShoppingCartIcon className="w-5 h-5" />
                  <span>购物车</span>
                </Link>
                <Link to="/orders" className="flex items-center gap-1.5 text-gray-600 hover:text-emerald-600 transition-colors">
                  <ClipboardDocumentListIcon className="w-5 h-5" />
                  <span>我的订单</span>
                </Link>
              </>
            )}
          </nav>

          {/* 用户区 */}
          <div className="flex items-center gap-3">
            {token ? (
              <>
                <span className="text-sm text-gray-500 hidden sm:inline">{user?.nickname || user?.phone}</span>
                <button onClick={handleLogout} className="text-sm text-gray-400 hover:text-red-500 transition-colors">
                  退出
                </button>
              </>
            ) : (
              <Link to="/login" className="btn-primary text-sm !px-4 !py-2">登录</Link>
            )}
            {/* 移动端菜单按钮 */}
            <button className="md:hidden p-2" onClick={() => setMenuOpen(!menuOpen)}>
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={menuOpen ? 'M6 18L18 6M6 6l12 12' : 'M4 6h16M4 12h16M4 18h16'} />
              </svg>
            </button>
          </div>
        </div>

        {/* 移动端菜单 */}
        {menuOpen && (
          <div className="md:hidden border-t border-gray-100 bg-white px-4 py-3 space-y-2">
            <Link to="/" className="block py-2 text-gray-600" onClick={() => setMenuOpen(false)}>首页</Link>
            {token && (
              <>
                <Link to="/cart" className="block py-2 text-gray-600" onClick={() => setMenuOpen(false)}>购物车</Link>
                <Link to="/orders" className="block py-2 text-gray-600" onClick={() => setMenuOpen(false)}>我的订单</Link>
              </>
            )}
          </div>
        )}
      </header>

      {/* 主体内容 */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* 底部 */}
      <footer className="border-t border-gray-100 bg-white py-6 text-center text-sm text-gray-400">
        社区生鲜配送 · 鲜送达 © 2026
      </footer>
    </div>
  );
}
