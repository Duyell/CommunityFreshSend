import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import * as productApi from '../api/product';
import * as cartApi from '../api/cart';
import type { ProductVO, CategoryVO } from '../types';

export default function Home() {
  const [products, setProducts] = useState<ProductVO[]>([]);
  const [total, setTotal] = useState(0);
  const [categories, setCategories] = useState<CategoryVO[]>([]);
  const [page, setPage] = useState(1);
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [sort, setSort] = useState('time');
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [addingSku, setAddingSku] = useState<number | null>(null);
  const token = localStorage.getItem('token');
  const size = 12;

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const fn = keyword ? productApi.searchProducts : productApi.getProducts;
      const result = await fn({ page, size, categoryId, sort, keyword } as Parameters<typeof productApi.getProducts>[0]);
      setProducts(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  }, [page, categoryId, sort, keyword]);

  useEffect(() => {
    productApi.getCategories().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setKeyword(searchInput);
    setPage(1);
  };

  const handleAddToCart = async (skuId: number) => {
    if (!token) {
      window.location.href = '/login';
      return;
    }
    setAddingSku(skuId);
    try {
      await cartApi.addToCart(skuId, 1);
    } finally {
      setAddingSku(null);
    }
  };

  const totalPages = Math.ceil(total / size);

  // 将分类平铺展示
  const flatCategories = categories.flatMap((c) => [c, ...(c.children || [])]);

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      {/* 搜索栏 */}
      <form onSubmit={handleSearch} className="mb-6">
        <div className="flex gap-3">
          <input
            type="text"
            className="input flex-1"
            placeholder="搜索生鲜商品..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <button type="submit" className="btn-primary">搜索</button>
        </div>
      </form>

      {/* 分类标签 */}
      <div className="flex flex-wrap gap-2 mb-6">
        <button
          onClick={() => { setCategoryId(undefined); setPage(1); }}
          className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
            !categoryId ? 'bg-emerald-600 text-white' : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
          }`}
        >
          全部
        </button>
        {flatCategories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => { setCategoryId(cat.id); setPage(1); }}
            className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
              categoryId === cat.id ? 'bg-emerald-600 text-white' : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
            }`}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {/* 排序 */}
      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-gray-500">共 {total} 件商品</span>
        <div className="flex gap-2">
          {[
            { value: 'time', label: '最新' },
            { value: 'price_asc', label: '价格↑' },
            { value: 'price_desc', label: '价格↓' },
          ].map((opt) => (
            <button
              key={opt.value}
              onClick={() => { setSort(opt.value); setPage(1); }}
              className={`px-3 py-1.5 rounded-lg text-sm transition-all ${
                sort === opt.value ? 'bg-emerald-100 text-emerald-700 font-medium' : 'text-gray-500 hover:bg-gray-100'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 商品网格 */}
      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="bg-white rounded-2xl p-4 animate-pulse">
              <div className="bg-gray-200 rounded-xl h-40 mb-3" />
              <div className="bg-gray-200 h-4 rounded w-3/4 mb-2" />
              <div className="bg-gray-200 h-4 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {products.map((product) => (
            <div key={product.id} className="bg-white rounded-2xl overflow-hidden border border-gray-100 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300">
              <Link to={`/product/${product.id}`}>
                <div className="aspect-square bg-gradient-to-br from-emerald-50 to-green-50 flex items-center justify-center p-6">
                  {product.images ? (
                    <img src={product.images} alt={product.name} className="w-full h-full object-cover rounded-xl" />
                  ) : (
                    <span className="text-5xl">🥬</span>
                  )}
                </div>
              </Link>
              <div className="p-4">
                <Link to={`/product/${product.id}`}>
                  <h3 className="font-medium text-gray-800 truncate">{product.name}</h3>
                </Link>
                <div className="flex items-center justify-between mt-2">
                  <div>
                    <span className="text-lg font-bold text-red-500">¥{product.minPrice?.toFixed(2)}</span>
                    {product.nearExpiryDiscount && (
                      <span className="ml-2 text-xs text-orange-500 line-through">
                        ¥{(product.nearExpiryDiscount * 1.4).toFixed(2)}
                      </span>
                    )}
                  </div>
                  {product.nearExpiryDiscount && (
                    <span className="badge bg-orange-100 text-orange-700">临期特价</span>
                  )}
                </div>
                <div className="flex items-center justify-between mt-3">
                  <span className="text-xs text-gray-400">库存 {product.totalStock}</span>
                  <button
                    onClick={(e) => { e.preventDefault(); handleAddToCart(product.skus[0]?.id); }}
                    disabled={addingSku === product.skus[0]?.id || !product.skus[0]}
                    className="bg-emerald-600 text-white text-sm px-4 py-1.5 rounded-lg hover:bg-emerald-700 active:scale-95 transition-all disabled:opacity-50"
                  >
                    {addingSku === product.skus[0]?.id ? '...' : '+ 加购'}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <button
            onClick={() => setPage(Math.max(1, page - 1))}
            disabled={page === 1}
            className="btn-outline !px-3 !py-1.5 text-sm"
          >
            上一页
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
            <button
              key={p}
              onClick={() => setPage(p)}
              className={`w-10 h-10 rounded-xl text-sm font-medium transition-all ${
                p === page ? 'bg-emerald-600 text-white' : 'text-gray-500 hover:bg-gray-100'
              }`}
            >
              {p}
            </button>
          ))}
          <button
            onClick={() => setPage(Math.min(totalPages, page + 1))}
            disabled={page === totalPages}
            className="btn-outline !px-3 !py-1.5 text-sm"
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}
