// ==================== 通用 ====================
export interface User {
  userId: number;
  phone: string;
  nickname: string;
  avatar: string | null;
  token: string;
  roles: string[];
}

// ==================== 商品 ====================
export interface SkuVO {
  id: number;
  specName: string;
  price: number;
}

export interface ProductVO {
  id: number;
  categoryId: number;
  name: string;
  description: string;
  images: string;
  status: number;
  isWeighted: number;
  minPrice: number;
  totalStock: number;
  nearExpiryDiscount: number | null;
  createTime: string;
  skus: SkuVO[];
}

export interface CategoryVO {
  id: number;
  name: string;
  parentId: number | null;
  sort: number;
  children: CategoryVO[];
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// ==================== 购物车 ====================
export interface CartItemVO {
  skuId: number;
  productId: number;
  productName: string;
  productImage: string;
  specName: string;
  price: number;
  quantity: number;
  stock: number;
  stockSufficient: boolean;
}

// ==================== 地址 ====================
export interface AddressVO {
  id: number;
  userId: number;
  contact: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: number;
}

// ==================== 订单 ====================
export interface OrderItemVO {
  id: number;
  productId: number;
  skuId: number;
  productName: string;
  specName: string;
  productImage: string;
  price: number;
  quantity: number;
  amount: number;
  shortage: boolean;
}

export interface OrderVO {
  id: number;
  orderNo: string;
  userId: number;
  addressId: number | null;
  pickupPointId: number | null;
  deliveryType: number;
  deliveryTimeSlot: string;
  status: number;
  statusText: string;
  totalAmount: number;
  deliveryFee: number;
  packageFee: number;
  couponDiscount: number;
  actualAmount: number;
  pickupCode: string;
  remark: string;
  cancelReason: string;
  paidTime: string | null;
  deliveredTime: string | null;
  createTime: string;
  items: OrderItemVO[];
}

export interface OrderCreateDTO {
  addressId?: number;
  pickupPointId?: number;
  deliveryType: number;
  deliveryTimeSlot?: string;
  remark?: string;
  userCouponId?: number;
}

// ==================== 优惠券 ====================
export interface UserCouponVO {
  id: number;
  couponId: number;
  couponName: string;
  type: number;
  typeText: string;
  threshold: number;
  discountValue: number;
  discountAmount: number | null;
  status: number;
  statusText: string;
  expireTime: string;
}

// ==================== API 响应 ====================
export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}
