import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as orderApi from '../api/order';
import type { OrderVO } from '../types';

const STATUS_COLORS: Record<number, string> = {
  0: 'bg-yellow-100 text-yellow-700',
  1: 'bg-blue-100 text-blue-700',
  2: 'bg-indigo-100 text-indigo-700',
  3: 'bg-purple-100 text-purple-700',
  4: 'bg-cyan-100 text-cyan-700',
  5: 'bg-green-100 text-green-700',
  6: 'bg-teal-100 text-teal-700',
  7: 'bg-orange-100 text-orange-700',
  8: 'bg-gray-100 text-gray-600',
  9: 'bg-red-100 text-red-700',
};

export default function Orders() {
  const [orders, setOrders] = useState<OrderVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  const loadOrders = useCallback(async () => {
    if (!token) { navigate('/login'); return; }
    setLoading(true);
    try {
      const result = await orderApi.getOrders({ page, size: 10, status: statusFilter });
      setOrders(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, token, navigate]);

  useEffect(() => { loadOrders(); }, [loadOrders]);

  const handlePay = async (orderId: number) => {
    try {
      await orderApi.payOrder(orderId);
      loadOrders();
    } catch {
      // error handled by interceptor
    }
  };

  const handleCancel = async (orderId: number) => {
    try {
      await orderApi.cancelOrder(orderId);
      loadOrders();
    } catch {
      // error handled by interceptor
    }
  };

  const statusTabs = [
    { label: '全部', value: undefined },
    { label: '待付款', value: 0 },
    { label: '待配送', value: 3 },
    { label: '配送中', value: 4 },
    { label: '已完成', value: 8 },
  ];

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-xl font-bold text-gray-800 mb-6">我的订单</h1>

      {/* 状态筛选 */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
        {statusTabs.map((tab) => (
          <button
            key={tab.label}
            onClick={() => { setStatusFilter(tab.value); setPage(1); }}
            className={`px-4 py-2 rounded-full text-sm whitespace-nowrap transition-all ${
              statusFilter === tab.value
                ? 'bg-emerald-600 text-white'
                : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 订单列表 */}
      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-32 bg-gray-200 rounded-2xl" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-16">
          <span className="text-5xl">📦</span>
          <p className="text-gray-400 mt-3">暂无订单</p>
          <Link to="/" className="btn-primary mt-4 inline-block">去逛逛</Link>
        </div>
      ) : (
        <div className="space-y-3">
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              className="card block hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-gray-400">{order.orderNo}</span>
                <span className={`badge ${STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-600'}`}>
                  {order.statusText}
                </span>
              </div>
              <div className="space-y-1">
                {order.items.slice(0, 2).map((item) => (
                  <div key={item.id} className="flex items-center gap-3 text-sm">
                    <span className="w-8 h-8 bg-emerald-50 rounded-lg flex items-center justify-center text-sm">🥬</span>
                    <span className="text-gray-600 truncate flex-1">{item.productName} ×{item.quantity}</span>
                    <span className="text-gray-800">¥{item.amount.toFixed(2)}</span>
                  </div>
                ))}
                {order.items.length > 2 && (
                  <p className="text-xs text-gray-400 pl-11">...等{order.items.length}件商品</p>
                )}
              </div>
              <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-50">
                <span className="text-sm text-gray-400">
                  {new Date(order.createTime).toLocaleDateString('zh-CN')}
                </span>
                <span className="font-bold text-lg">
                  ¥{order.actualAmount.toFixed(2)}
                </span>
              </div>
              {/* 快捷操作 */}
              <div className="flex gap-2 mt-2" onClick={(e) => e.preventDefault()}>
                {order.status === 0 && (
                  <>
                    <button onClick={() => handlePay(order.id)} className="btn-primary !py-1.5 !px-4 text-sm flex-1">去支付</button>
                    <button onClick={() => handleCancel(order.id)} className="btn-outline !py-1.5 !px-4 text-sm">取消</button>
                  </>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* 分页 */}
      {total > 10 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1} className="btn-outline !px-3 !py-1.5 text-sm">上一页</button>
          <span className="text-sm text-gray-500">{page} / {Math.ceil(total / 10)}</span>
          <button onClick={() => setPage((p) => p + 1)} disabled={page * 10 >= total} className="btn-outline !px-3 !py-1.5 text-sm">下一页</button>
        </div>
      )}
    </div>
  );
}
