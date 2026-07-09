import client from './client';
import type { ApiResult, ProductVO, CategoryVO, PageResult } from '../types';

export async function getCategories(): Promise<CategoryVO[]> {
  const res = await client.get<ApiResult<CategoryVO[]>>('/category/tree');
  return res.data.data;
}

export async function getProducts(params: {
  page?: number;
  size?: number;
  categoryId?: number;
  sort?: string;
}): Promise<PageResult<ProductVO>> {
  const res = await client.get<ApiResult<PageResult<ProductVO>>>('/product/list', { params });
  return res.data.data;
}

export async function searchProducts(params: {
  page?: number;
  size?: number;
  categoryId?: number;
  keyword: string;
  sort?: string;
}): Promise<PageResult<ProductVO>> {
  const res = await client.get<ApiResult<PageResult<ProductVO>>>('/product/search', { params });
  return res.data.data;
}

export async function getProductDetail(id: number): Promise<ProductVO> {
  const res = await client.get<ApiResult<ProductVO>>(`/product/${id}`);
  return res.data.data;
}
