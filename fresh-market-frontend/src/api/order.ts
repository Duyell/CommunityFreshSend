import client from './client';
import type { ApiResult, OrderVO, OrderCreateDTO, PageResult, UserCouponVO } from '../types';

export async function createOrder(dto: OrderCreateDTO): Promise<OrderVO> {
  const res = await client.post<ApiResult<OrderVO>>('/order', dto);
  return res.data.data;
}

export async function payOrder(orderId: number): Promise<void> {
  await client.post(`/order/${orderId}/pay`);
}

export async function cancelOrder(orderId: number, reason?: string): Promise<void> {
  await client.put(`/order/${orderId}/cancel`, null, { params: { reason } });
}

export async function getOrders(params: {
  page?: number;
  size?: number;
  status?: number;
}): Promise<PageResult<OrderVO>> {
  const res = await client.get<ApiResult<PageResult<OrderVO>>>('/order/page', { params });
  return res.data.data;
}

export async function getOrderDetail(id: number): Promise<OrderVO> {
  const res = await client.get<ApiResult<OrderVO>>(`/order/${id}`);
  return res.data.data;
}

export async function getAvailableCoupons(
  orderAmount: number,
  categoryIds: number[],
): Promise<UserCouponVO[]> {
  const res = await client.get<ApiResult<UserCouponVO[]>>('/coupon/available', {
    params: { orderAmount, categoryIds: categoryIds.join(',') },
  });
  return res.data.data;
}
