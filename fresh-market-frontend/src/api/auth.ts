import client from './client';
import type { ApiResult, User } from '../types';

export async function login(phone: string, password: string): Promise<User> {
  const res = await client.post<ApiResult<User>>('/auth/login', { phone, password });
  return res.data.data;
}

export async function register(phone: string, password: string, nickname: string): Promise<User> {
  const res = await client.post<ApiResult<User>>('/auth/register', { phone, password, nickname });
  return res.data.data;
}
