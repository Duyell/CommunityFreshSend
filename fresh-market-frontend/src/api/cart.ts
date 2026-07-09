import client from './client';
import type { ApiResult, CartItemVO } from '../types';

export async function getCart(): Promise<CartItemVO[]> {
  const res = await client.get<ApiResult<CartItemVO[]>>('/cart/list');
  return res.data.data;
}

export async function addToCart(skuId: number, quantity: number): Promise<void> {
  await client.post('/cart/add', { skuId, quantity });
}

export async function updateCartQty(skuId: number, quantity: number): Promise<void> {
  await client.put('/cart/update', { skuId, quantity });
}

export async function removeFromCart(skuId: number): Promise<void> {
  await client.delete(`/cart/remove/${skuId}`);
}

export async function clearCart(): Promise<void> {
  await client.delete('/cart/clear');
}
