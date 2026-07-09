import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
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

const DELIVERY_TYPE_MAP: Record<number, string> = { 1: '配送到家', 2: '到自提点取货' };

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<OrderVO | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  useEffect(() => {
    if (id && token) {
      orderApi.getOrderDetail(Number(id)).then(setOrder).finally(() => setLoading(false));
    } else {
      navigate('/login');
    }
  }, [id, token, navigate]);

  const handlePay = async () => {
    if (!order) return;
    await orderApi.payOrder(order.id);
    setOrder(await orderApi.getOrderDetail(order.id));
  };

  const handleCancel = async () => {
    if (!order) return;
    await orderApi.cancelOrder(order.id);
    setOrder(await orderApi.getOrderDetail(order.id));
  };

  if (loading) {
    return (
      <div className="max-w-lg mx-auto px-4 py-8 animate-pulse space-y-4">
        <div className="h-8 bg-gray-200 rounded w-1/2" />
        <div className="h-40 bg-gray-200 rounded-2xl" />
      </div>
    );
  }

  if (!order) {
    return <div className="text-center py-20 text-gray-400">订单不存在</div>;
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
      {/* 状态 */}
      <div className="card text-center">
        <span className={`badge text-lg px-4 py-1.5 ${STATUS_COLORS[order.status] || ''}`}>
          {order.statusText}
        </span>
        {order.pickupCode && (
          <p className="mt-3 text-2xl font-mono font-bold tracking-widest text-emerald-700">
            取货码: {order.pickupCode}
          </p>
        )}
        {order.cancelReason && (
          <p className="mt-2 text-sm text-gray-400">原因: {order.cancelReason}</p>
        )}
      </div>

      {/* 订单信息 */}
      <div className="card space-y-2 text-sm">
        <div className="flex justify-between"><span className="text-gray-500">订单编号</span><span className="font-mono">{order.orderNo}</span></div>
        <div className="flex justify-between"><span className="text-gray-500">配送方式</span><span>{DELIVERY_TYPE_MAP[order.deliveryType] || '未知'}</span></div>
        {order.deliveryTimeSlot && (
          <div className="flex justify-between"><span className="text-gray-500">配送时间</span><span>{order.deliveryTimeSlot}</span></div>
        )}
        <div className="flex justify-between"><span className="text-gray-500">下单时间</span><span>{new Date(order.createTime).toLocaleString('zh-CN')}</span></div>
        {order.paidTime && (
          <div className="flex justify-between"><span className="text-gray-500">支付时间</span><span>{new Date(order.paidTime).toLocaleString('zh-CN')}</span></div>
        )}
        {order.deliveredTime && (
          <div className="flex justify-between"><span className="text-gray-500">送达时间</span><span>{new Date(order.deliveredTime).toLocaleString('zh-CN')}</span></div>
        )}
        {order.remark && (
          <div className="flex justify-between"><span className="text-gray-500">备注</span><span>{order.remark}</span></div>
        )}
      </div>

      {/* 商品明细 */}
      <div className="card">
        <h2 className="font-medium text-gray-700 mb-3">商品明细</h2>
        <div className="space-y-3">
          {order.items.map((item) => (
            <div key={item.id} className="flex items-center gap-3">
              <div className="w-12 h-12 bg-emerald-50 rounded-xl flex items-center justify-center text-xl flex-shrink-0">🥬</div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-800 truncate">{item.productName}</p>
                <p className="text-xs text-gray-400">{item.specName}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-gray-800">¥{item.price.toFixed(2)} × {item.quantity}</p>
                <p className="text-sm font-bold text-gray-800">¥{item.amount.toFixed(2)}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 费用明细 */}
      <div className="card space-y-2 text-sm">
        <div className="flex justify-between"><span className="text-gray-500">商品小计</span><span>¥{order.totalAmount.toFixed(2)}</span></div>
        <div className="flex justify-between"><span className="text-gray-500">配送费</span><span>¥{order.deliveryFee.toFixed(2)}</span></div>
        <div className="flex justify-between"><span className="text-gray-500">包装费</span><span>¥{order.packageFee.toFixed(2)}</span></div>
        {order.couponDiscount > 0 && (
          <div className="flex justify-between"><span className="text-gray-500">优惠券</span><span className="text-emerald-600">-¥{order.couponDiscount.toFixed(2)}</span></div>
        )}
        <div className="flex justify-between text-lg font-bold pt-2 border-t border-gray-100">
          <span>实付</span>
          <span className="text-red-500">¥{order.actualAmount.toFixed(2)}</span>
        </div>
      </div>

      {/* 操作按钮 */}
      {order.status === 0 && (
        <div className="flex gap-3">
          <button onClick={handlePay} className="btn-primary flex-1 py-3">去支付</button>
          <button onClick={handleCancel} className="btn-outline flex-1 py-3">取消订单</button>
        </div>
      )}
      {order.status === 8 && (
        <Link to="/" className="btn-primary block text-center py-3">再来一单</Link>
      )}
    </div>
  );
}
