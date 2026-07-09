package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.ReviewCreateDTO;
import com.duyell.communityfreshdelivery.dto.ReviewVO;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.entity.Review;
import com.duyell.communityfreshdelivery.entity.User;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.ReviewMapper;
import com.duyell.communityfreshdelivery.mapper.UserMapper;
import com.duyell.communityfreshdelivery.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>评价服务实现</h2>
 *
 * <h3>评价规则</h3>
 * <ul>
 *   <li>只能评价自己的订单</li>
 *   <li>订单状态必须为已签收(5)或用户已自提(6)</li>
 *   <li>同一订单同一商品只能评价一次</li>
 *   <li>评价后订单状态自动流转为已完成(8)</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO create(ReviewCreateDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        // 1. 校验订单存在且属于当前用户
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(20006, "订单不存在");
        }

        // 2. 先校验是否已评价（同一订单+商品），再校验状态
        boolean exists = reviewMapper.exists(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, dto.getOrderId())
                        .eq(Review::getProductId, dto.getProductId())
                        .eq(Review::getUserId, userId)
        );
        if (exists) {
            throw new BusinessException(50003, "该商品已评价过，不可重复评价");
        }

        // 3. 校验订单状态（已签收/已自提/待评价 才能评价）
        int status = order.getStatus();
        if (status != OrderStatus.RECEIVED.getCode()
                && status != OrderStatus.PICKED_UP.getCode()
                && status != OrderStatus.PENDING_REVIEW.getCode()) {
            throw new BusinessException(50001, "当前订单状态不可评价");
        }

        // 4. 校验订单包含该商品
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, dto.getOrderId())
                        .eq(OrderItem::getProductId, dto.getProductId())
        );
        if (items.isEmpty()) {
            throw new BusinessException(50002, "该商品不在订单中");
        }

        // 5. 创建评价
        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setUserId(userId);
        review.setProductId(dto.getProductId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent() != null ? dto.getContent() : "");
        review.setImages(dto.getImages() != null ? dto.getImages() : "");
        reviewMapper.insert(review);

        // 6. 若订单所有商品均已评价，则完成订单
        long totalItems = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, dto.getOrderId())
        );
        long reviewedCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, dto.getOrderId())
        );
        if (reviewedCount >= totalItems) {
            order.setStatus(OrderStatus.COMPLETED.getCode());
        } else {
            order.setStatus(OrderStatus.PENDING_REVIEW.getCode());
        }
        orderMapper.updateById(order);

        log.info("评价创建成功: userId={} orderNo={} productId={} score={}",
                userId, order.getOrderNo(), dto.getProductId(), dto.getScore());

        return toVO(review);
    }

    @Override
    public List<ReviewVO> getByOrderId(Long orderId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .orderByDesc(Review::getCreateTime)
        );
        return toVOList(reviews);
    }

    @Override
    public Page<ReviewVO> getByProductId(Long productId, int page, int size) {
        Page<Review> result = reviewMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .orderByDesc(Review::getCreateTime)
        );
        return toVOPage(result);
    }

    @Override
    public Page<ReviewVO> myReviews(int page, int size) {
        Long userId = SecurityUtil.currentUserId();

        Page<Review> result = reviewMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .orderByDesc(Review::getCreateTime)
        );
        return toVOPage(result);
    }

    // ==================== 内部方法 ====================

    /** Entity 分页 → VO 分页 */
    private Page<ReviewVO> toVOPage(Page<Review> result) {
        List<ReviewVO> vos = toVOList(result.getRecords());
        Page<ReviewVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    private List<ReviewVO> toVOList(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量加载用户昵称
        Set<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = userMapper.selectList(
                        new LambdaQueryWrapper<User>().in(User::getId, userIds)
                ).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        // 批量加载商品名称
        Set<Long> productIds = reviews.stream()
                .map(Review::getProductId)
                .collect(Collectors.toSet());
        Map<Long, String> productNameMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        return reviews.stream()
                .map(r -> ReviewVO.builder()
                        .id(r.getId())
                        .orderId(r.getOrderId())
                        .userId(r.getUserId())
                        .userNickname(nicknameMap.getOrDefault(r.getUserId(), ""))
                        .productId(r.getProductId())
                        .productName(productNameMap.getOrDefault(r.getProductId(), ""))
                        .score(r.getScore())
                        .content(r.getContent())
                        .images(r.getImages())
                        .createTime(r.getCreateTime())
                        .build())
                .toList();
    }

    private ReviewVO toVO(Review review) {
        return toVOList(List.of(review)).getFirst();
    }
}
