import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import * as productApi from '../api/product';
import * as cartApi from '../api/cart';
import type { ProductVO } from '../types';

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<ProductVO | null>(null);
  const [selectedSku, setSelectedSku] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  useEffect(() => {
    if (id) {
      productApi.getProductDetail(Number(id)).then((p) => {
        setProduct(p);
        if (p.skus.length > 0) setSelectedSku(p.skus[0].id);
        setLoading(false);
      });
    }
  }, [id]);

  const handleAddToCart = async () => {
    if (!token) { navigate('/login'); return; }
    if (!selectedSku) return;
    setAdding(true);
    try {
      await cartApi.addToCart(selectedSku, quantity);
      setAdded(true);
      setTimeout(() => setAdded(false), 2000);
    } finally {
      setAdding(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 animate-pulse">
        <div className="flex flex-col md:flex-row gap-8">
          <div className="w-full md:w-1/2 aspect-square bg-gray-200 rounded-2xl" />
          <div className="flex-1 space-y-4">
            <div className="h-8 bg-gray-200 rounded w-3/4" />
            <div className="h-6 bg-gray-200 rounded w-1/4" />
            <div className="h-20 bg-gray-200 rounded" />
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return <div className="text-center py-20 text-gray-400">商品不存在</div>;
  }

  const selectedSkuData = product.skus.find((s) => s.id === selectedSku);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex flex-col md:flex-row gap-8">
        {/* 商品图片 */}
        <div className="w-full md:w-1/2">
          <div className="aspect-square bg-gradient-to-br from-emerald-50 to-green-50 rounded-2xl flex items-center justify-center p-8">
            {product.images ? (
              <img src={product.images} alt={product.name} className="w-full h-full object-cover rounded-xl" />
            ) : (
              <span className="text-8xl">🥬</span>
            )}
          </div>
        </div>

        {/* 商品信息 */}
        <div className="flex-1 space-y-5">
          <h1 className="text-2xl font-bold text-gray-800">{product.name}</h1>

          <div className="flex items-baseline gap-3">
            <span className="text-3xl font-bold text-red-500">
              ¥{selectedSkuData?.price ? selectedSkuData.price.toFixed(2) : product.minPrice?.toFixed(2)}
            </span>
            {product.nearExpiryDiscount && (
              <span className="text-sm text-gray-400 line-through">原价 ¥{(product.nearExpiryDiscount * 1.4).toFixed(2)}</span>
            )}
            {product.nearExpiryDiscount && (
              <span className="badge bg-orange-100 text-orange-700">临期 {product.nearExpiryDiscount.toFixed(2)}</span>
            )}
          </div>

          <p className="text-gray-500 leading-relaxed">{product.description || '新鲜直达，品质保证'}</p>

          <div className="text-sm text-gray-400">
            {product.isWeighted === 1 ? '称重商品（按实际重量计价）' : '固定规格'} · 库存 {product.totalStock}
          </div>

          {/* SKU 选择 */}
          {product.skus.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-2">规格</label>
              <div className="flex flex-wrap gap-2">
                {product.skus.map((sku) => (
                  <button
                    key={sku.id}
                    onClick={() => setSelectedSku(sku.id)}
                    className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                      selectedSku === sku.id
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {sku.specName} ¥{sku.price.toFixed(2)}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 数量 */}
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-2">数量</label>
            <div className="flex items-center gap-3">
              <button
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                className="w-10 h-10 rounded-xl bg-gray-100 text-gray-600 hover:bg-gray-200 text-lg transition-colors"
              >
                −
              </button>
              <span className="w-12 text-center font-medium text-lg">{quantity}</span>
              <button
                onClick={() => setQuantity(quantity + 1)}
                className="w-10 h-10 rounded-xl bg-gray-100 text-gray-600 hover:bg-gray-200 text-lg transition-colors"
              >
                +
              </button>
            </div>
          </div>

          {/* 操作按钮 */}
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleAddToCart}
              disabled={adding || !selectedSku}
              className={`flex-1 py-3 rounded-xl font-medium text-lg transition-all ${
                added
                  ? 'bg-green-100 text-green-700'
                  : 'btn-primary'
              }`}
            >
              {added ? '✓ 已加入购物车' : adding ? '...' : '加入购物车'}
            </button>
            <button
              onClick={() => { handleAddToCart().then(() => navigate('/cart')); }}
              disabled={adding || !selectedSku}
              className="btn-outline flex-1 py-3 rounded-xl font-medium text-lg"
            >
              立即购买
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
