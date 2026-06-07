package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.CartItemVO;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.entity.ProductSku;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.duyell.communityfreshdelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>购物车服务实现</h2>
 *
 * <h3>Redis 结构</h3>
 * <pre>{@code
 * Key:   cart:user:{userId}
 * Type:  Hash
 * Field: skuId（字符串）
 * Value: 数量（字符串）
 * }</pre>
 *
 * <h3>关键设计</h3>
 * <ul>
 *   <li><b>不扣库存</b> — 加购仅校验 SKU 存在、status=1、stock>0，不下扣库存</li>
 *   <li><b>用户隔离</b> — 从 SecurityContext 取当前用户 ID</li>
 *   <li><b>列表组装</b> — 从 Redis 取 skuId→quantity，批量查 ProductSku + Product 拼出完整信息</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate redisTemplate;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    private static final String CART_KEY_PREFIX = "cart:user:";

    private String cartKey() {
        return CART_KEY_PREFIX + SecurityUtil.currentUserId();
    }

    @Override
    public void add(CartAddDTO dto) {
        validateSku(dto.getSkuId());

        String existing = redisTemplate.<String, String>opsForHash()
                .get(cartKey(), dto.getSkuId().toString());
        int newQuantity = dto.getQuantity();
        if (existing != null) {
            newQuantity = Integer.parseInt(existing) + dto.getQuantity();
        }

        redisTemplate.opsForHash().put(cartKey(),
                dto.getSkuId().toString(), String.valueOf(newQuantity));
        log.info("加购成功: userId={} skuId={} quantity={}", SecurityUtil.currentUserId(), dto.getSkuId(), newQuantity);
    }

    @Override
    public void updateQty(CartAddDTO dto) {
        String key = cartKey();
        Boolean exists = redisTemplate.opsForHash().hasKey(key, dto.getSkuId().toString());
        if (!exists) {
            throw new BusinessException(10002, "购物车中不存在该商品");
        }
        redisTemplate.opsForHash().put(key,
                dto.getSkuId().toString(), String.valueOf(dto.getQuantity()));
        log.info("购物车数量更新: userId={} skuId={} quantity={}", SecurityUtil.currentUserId(), dto.getSkuId(), dto.getQuantity());
    }

    @Override
    public void remove(Long skuId) {
        redisTemplate.opsForHash().delete(cartKey(), skuId.toString());
        log.info("购物车删除: userId={} skuId={}", SecurityUtil.currentUserId(), skuId);
    }

    @Override
    public void clear() {
        redisTemplate.delete(cartKey());
        log.info("购物车已清空: userId={}", SecurityUtil.currentUserId());
    }

    @Override
    public List<CartItemVO> list() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey());
        if (entries.isEmpty()) {
            return List.of();
        }

        // 批量查出所有 SKU
        Set<Long> skuIds = entries.keySet().stream()
                .map(k -> Long.valueOf(k.toString()))
                .collect(Collectors.toSet());
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIds)
        );
        Map<Long, ProductSku> skuMap = skus.stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));

        // 批量查出对应商品
        Set<Long> productIds = skus.stream()
                .map(ProductSku::getProductId)
                .collect(Collectors.toSet());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
        );
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItemVO> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long skuId = Long.valueOf(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());
            ProductSku sku = skuMap.get(skuId);
            if (sku == null) {
                continue;
            }
            Product product = productMap.get(sku.getProductId());

            String firstImage = null;
            if (product != null && product.getImages() != null && !product.getImages().isBlank()) {
                firstImage = product.getImages();
            }

            items.add(CartItemVO.builder()
                    .skuId(skuId)
                    .productId(sku.getProductId())
                    .productName(product != null ? product.getName() : "未知商品")
                    .productImage(firstImage)
                    .specName(sku.getSpecName())
                    .price(sku.getPrice())
                    .quantity(quantity)
                    .stock(sku.getStock())
                    .stockSufficient(sku.getStock() >= quantity)
                    .build());
        }
        return items;
    }

    /** 校验 SKU 存在且可购买 */
    private void validateSku(Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == 0) {
            throw new BusinessException(10002, "商品规格不存在或已下架");
        }
        if (sku.getStock() <= 0) {
            throw new BusinessException(10003, "商品库存不足");
        }
    }
}
