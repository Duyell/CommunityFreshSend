package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.FavoriteVO;
import com.duyell.communityfreshdelivery.entity.Favorite;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.entity.ProductSku;
import com.duyell.communityfreshdelivery.mapper.FavoriteMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.duyell.communityfreshdelivery.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>收藏夹服务实现</h2>
 *
 * <p>列表查询时批量加载商品信息（名称/最低价/首图），消除 N+1.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public void add(Long productId) {
        Long userId = SecurityUtil.currentUserId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(10001, "商品不存在");
        }

        boolean exists = favoriteMapper.exists(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );
        if (exists) {
            throw new BusinessException(90001, "已收藏过该商品");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);

        log.info("收藏商品: userId={} productId={}", userId, productId);
    }

    @Override
    public void remove(Long productId) {
        Long userId = SecurityUtil.currentUserId();

        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );

        log.info("取消收藏: userId={} productId={}", userId, productId);
    }

    @Override
    public List<FavoriteVO> list() {
        Long userId = SecurityUtil.currentUserId();

        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime)
        );

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        return toVOList(favorites);
    }

    @Override
    public boolean isFavorited(Long productId) {
        Long userId = SecurityUtil.currentUserId();

        return favoriteMapper.exists(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );
    }

    // ==================== 内部方法 ====================

    /** 批量 Entity → VO */
    private List<FavoriteVO> toVOList(List<Favorite> favorites) {
        // 批量加载商品信息
        Set<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> productMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 批量加载最低价（每个商品取 min(price)）
        Map<Long, BigDecimal> minPriceMap = Collections.emptyMap();
        if (!productIds.isEmpty()) {
            List<ProductSku> allSkus = productSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductSku>()
                            .in(ProductSku::getProductId, productIds)
                            .eq(ProductSku::getStatus, 1)
            );
            minPriceMap = allSkus.stream()
                    .collect(Collectors.groupingBy(
                            ProductSku::getProductId,
                            Collectors.collectingAndThen(
                                    Collectors.minBy(
                                            java.util.Comparator.comparing(ProductSku::getPrice)
                                    ),
                                    opt -> opt.map(ProductSku::getPrice).orElse(BigDecimal.ZERO)
                            )
                    ));
        }

        Map<Long, BigDecimal> finalMinPriceMap = minPriceMap;
        return favorites.stream()
                .map(f -> {
                    Product p = productMap.get(f.getProductId());
                    String productName = p != null ? p.getName() : "未知商品";
                    String firstImage = null;
                    if (p != null && p.getImages() != null && !p.getImages().isBlank()) {
                        firstImage = p.getImages();
                    }
                    BigDecimal minPrice = finalMinPriceMap.getOrDefault(f.getProductId(), BigDecimal.ZERO);
                    Integer productStatus = p != null ? p.getStatus() : null;

                    return FavoriteVO.builder()
                            .id(f.getId())
                            .productId(f.getProductId())
                            .productName(productName)
                            .minPrice(minPrice)
                            .productImage(firstImage)
                            .productStatus(productStatus)
                            .createTime(f.getCreateTime())
                            .build();
                })
                .toList();
    }
}
