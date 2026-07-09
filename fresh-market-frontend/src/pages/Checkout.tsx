import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as cartApi from '../api/cart';
import * as orderApi from '../api/order';
import * as addressApi from '../api/address';
import type { CartItemVO, AddressVO, UserCouponVO } from '../types';

export default function Checkout() {
  const [items, setItems] = useState<CartItemVO[]>([]);
  const [addresses, setAddresses] = useState<AddressVO[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<number | null>(null);
  const [deliveryType, setDeliveryType] = useState(1); // 1=配送到家 2=自提
  const [timeSlot, setTimeSlot] = useState('上午(9-12)');
  const [remark, setRemark] = useState('');
  const [coupons, setCoupons] = useState<UserCouponVO[]>([]);
  const [selectedCoupon, setSelectedCoupon] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    cartApi.getCart().then(setItems).catch(() => {});
    addressApi.getAddresses().then(setAddresses).catch(() => {});
  }, []);

  const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  const categoryIds = [...new Set(items.map((i) => i.productId))]; // 简化：用 productId 代替 categoryId

  useEffect(() => {
    if (total > 0) {
      orderApi.getAvailableCoupons(total, categoryIds).then(setCoupons).catch(() => {});
    }
  }, [total]);

  const selectedCouponData = coupons.find((c) => c.id === selectedCoupon);
  const deliveryFee = total >= 30 ? 0 : 5;
  const packageFee = 1;
  const couponDiscount = selectedCouponData?.discountAmount || 0;
  const actualAmount = Math.max(0, total + deliveryFee + packageFee - couponDiscount);

  const handleSubmit = async () => {
    setError('');
    if (deliveryType === 1 && !selectedAddress) {
      setError('请选择收货地址');
      return;
    }
    setSubmitting(true);
    try {
      const dto = {
        addressId: deliveryType === 1 ? selectedAddress : undefined,
        deliveryType,
        deliveryTimeSlot: deliveryType === 1 ? timeSlot : undefined,
        remark: remark || undefined,
        userCouponId: selectedCoupon || undefined,
      };
      await orderApi.createOrder(dto);
      navigate('/orders');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { msg?: string } } })?.response?.data?.msg;
      setError(msg || '下单失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <span className="text-5xl">🛒</span>
        <p className="text-gray-500 mt-4">购物车为空，无法下单</p>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-6 space-y-6">
      <h1 className="text-xl font-bold text-gray-800">确认订单</h1>

      {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl text-sm">{error}</div>}

      {/* 配送方式 */}
      <div className="card">
        <h2 className="font-medium text-gray-700 mb-3">配送方式</h2>
        <div className="flex gap-3">
          {[
            { value: 1, label: '🚚 配送到家' },
            { value: 2, label: '📍 到自提点取货' },
          ].map((opt) => (
            <button
              key={opt.value}
              onClick={() => setDeliveryType(opt.value)}
              className={`flex-1 py-3 rounded-xl text-sm font-medium transition-all ${
                deliveryType === opt.value
                  ? 'bg-emerald-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 收货地址 */}
      {deliveryType === 1 && (
        <div className="card">
          <h2 className="font-medium text-gray-700 mb-3">收货地址</h2>
          {addresses.length === 0 ? (
            <p className="text-sm text-gray-400">暂无地址，请先添加收货地址</p>
          ) : (
            <div className="space-y-2">
              {addresses.map((addr) => (
                <button
                  key={addr.id}
                  onClick={() => setSelectedAddress(addr.id)}
                  className={`w-full text-left p-3 rounded-xl border transition-all ${
                    selectedAddress === addr.id
                      ? 'border-emerald-500 bg-emerald-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <p className="font-medium text-sm">{addr.contact} {addr.phone}</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {addr.province}{addr.city}{addr.district} {addr.detail}
                  </p>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 配送时间 */}
      {deliveryType === 1 && (
        <div className="card">
          <h2 className="font-medium text-gray-700 mb-3">配送时间</h2>
          <div className="flex flex-wrap gap-2">
            {['上午(9-12)', '下午(14-17)', '晚间(17-20)'].map((slot) => (
              <button
                key={slot}
                onClick={() => setTimeSlot(slot)}
                className={`px-4 py-2 rounded-xl text-sm transition-all ${
                  timeSlot === slot ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {slot}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 订单商品 */}
      <div className="card">
        <h2 className="font-medium text-gray-700 mb-3">商品 ({items.length}件)</h2>
        <div className="space-y-2">
          {items.map((item) => (
            <div key={item.skuId} className="flex items-center justify-between text-sm">
              <span className="text-gray-600 truncate flex-1">
                {item.productName} × {item.quantity}
              </span>
              <span className="text-gray-800 font-medium ml-2">¥{(item.price * item.quantity).toFixed(2)}</span>
            </div>
          ))}
        </div>
      </div>

      {/* 优惠券 */}
      {coupons.length > 0 && (
        <div className="card">
          <h2 className="font-medium text-gray-700 mb-3">优惠券</h2>
          <div className="space-y-2">
            <button
              onClick={() => setSelectedCoupon(null)}
              className={`w-full text-left p-2 rounded-lg text-sm ${
                !selectedCoupon ? 'bg-emerald-50 text-emerald-700' : 'text-gray-500'
              }`}
            >
              不使用优惠券
            </button>
            {coupons.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedCoupon(c.id)}
                className={`w-full text-left p-3 rounded-xl border transition-all ${
                  selectedCoupon === c.id
                    ? 'border-emerald-500 bg-emerald-50'
                    : 'border-gray-200'
                }`}
              >
                <p className="font-medium text-sm">{c.couponName}</p>
                <p className="text-xs text-gray-400">
                  满¥{c.threshold.toFixed(0)}减{c.discountAmount?.toFixed(2)}
                </p>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 备注 */}
      <div className="card">
        <h2 className="font-medium text-gray-700 mb-3">备注</h2>
        <input
          type="text"
          className="input"
          placeholder="给商家的备注（可选）"
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
        />
      </div>

      {/* 费用明细 */}
      <div className="card space-y-2 text-sm">
        <div className="flex justify-between"><span className="text-gray-500">商品小计</span><span>¥{total.toFixed(2)}</span></div>
        <div className="flex justify-between"><span className="text-gray-500">配送费</span><span>{deliveryFee === 0 ? <span className="text-emerald-600">免运费</span> : `¥${deliveryFee}`}</span></div>
        <div className="flex justify-between"><span className="text-gray-500">包装费</span><span>¥{packageFee}</span></div>
        {couponDiscount > 0 && (
          <div className="flex justify-between"><span className="text-gray-500">优惠券</span><span className="text-emerald-600">-¥{couponDiscount.toFixed(2)}</span></div>
        )}
        <div className="flex justify-between text-lg font-bold pt-2 border-t border-gray-100">
          <span>实付</span>
          <span className="text-red-500">¥{actualAmount.toFixed(2)}</span>
        </div>
      </div>

      {/* 提交按钮 */}
      <button
        onClick={handleSubmit}
        disabled={submitting}
        className="btn-primary w-full py-3 text-lg"
      >
        {submitting ? '提交中...' : `提交订单 ¥${actualAmount.toFixed(2)}`}
      </button>
    </div>
  );
}
