import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as cartApi from '../api/cart';
import type { CartItemVO } from '../types';

export default function Cart() {
  const [items, setItems] = useState<CartItemVO[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  const loadCart = useCallback(async () => {
    if (!token) { navigate('/login'); return; }
    setLoading(true);
    try {
      setItems(await cartApi.getCart());
    } finally {
      setLoading(false);
    }
  }, [token, navigate]);

  useEffect(() => { loadCart(); }, [loadCart]);

  const handleUpdateQty = async (skuId: number, qty: number) => {
    if (qty < 1) return;
    await cartApi.updateCartQty(skuId, qty);
    setItems((prev) => prev.map((i) => (i.skuId === skuId ? { ...i, quantity: qty } : i)));
  };

  const handleRemove = async (skuId: number) => {
    await cartApi.removeFromCart(skuId);
    setItems((prev) => prev.filter((i) => i.skuId !== skuId));
  };

  const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8 animate-pulse space-y-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-24 bg-gray-200 rounded-2xl" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <span className="text-6xl">🛒</span>
        <h2 className="text-xl font-bold text-gray-700 mt-4">购物车是空的</h2>
        <p className="text-gray-400 mt-2 mb-6">去逛逛新鲜的生鲜商品吧</p>
        <Link to="/" className="btn-primary">去逛逛</Link>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-xl font-bold text-gray-800 mb-6">购物车 ({items.length})</h1>

      <div className="space-y-3 mb-6">
        {items.map((item) => (
          <div key={item.skuId} className="card flex items-center gap-4">
            <div className="w-16 h-16 bg-gradient-to-br from-emerald-50 to-green-50 rounded-xl flex items-center justify-center flex-shrink-0 text-2xl">
              🥬
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="font-medium text-gray-800 truncate">{item.productName}</h3>
              <p className="text-sm text-gray-400">{item.specName}</p>
              <p className="text-red-500 font-bold mt-1">¥{item.price.toFixed(2)}</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => handleUpdateQty(item.skuId, item.quantity - 1)}
                className="w-8 h-8 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors"
              >
                −
              </button>
              <span className="w-8 text-center font-medium">{item.quantity}</span>
              <button
                onClick={() => handleUpdateQty(item.skuId, item.quantity + 1)}
                className="w-8 h-8 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors"
              >
                +
              </button>
            </div>
            <div className="text-right flex-shrink-0">
              <p className="font-bold text-gray-800">¥{(item.price * item.quantity).toFixed(2)}</p>
              <button
                onClick={() => handleRemove(item.skuId)}
                className="text-xs text-gray-400 hover:text-red-500 mt-1 transition-colors"
              >
                删除
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* 底部结算栏 */}
      <div className="sticky bottom-0 bg-white border-t border-gray-100 p-4 rounded-t-2xl shadow-lg">
        <div className="flex items-center justify-between mb-3">
          <span className="text-gray-500">合计</span>
          <span className="text-2xl font-bold text-red-500">¥{total.toFixed(2)}</span>
        </div>
        <button
          onClick={() => navigate('/checkout')}
          className="btn-primary w-full py-3 text-lg"
        >
          去结算
        </button>
      </div>
    </div>
  );
}
