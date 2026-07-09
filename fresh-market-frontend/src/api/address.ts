import client from './client';
import type { ApiResult, AddressVO } from '../types';

export async function getAddresses(): Promise<AddressVO[]> {
  const res = await client.get<ApiResult<AddressVO[]>>('/address/list');
  return res.data.data;
}

export async function createAddress(data: Omit<AddressVO, 'id' | 'userId'>): Promise<AddressVO> {
  const res = await client.post<ApiResult<AddressVO>>('/address', data);
  return res.data.data;
}

export async function deleteAddress(id: number): Promise<void> {
  await client.delete(`/address/${id}`);
}

export async function setDefaultAddress(id: number): Promise<void> {
  await client.put(`/address/${id}/default`);
}
