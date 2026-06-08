package com.duyell.communityfreshdelivery;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.dto.ReviewCreateDTO;
import com.duyell.communityfreshdelivery.dto.ReviewVO;
import com.duyell.communityfreshdelivery.entity.Review;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.mapper.ReviewMapper;
import com.duyell.communityfreshdelivery.service.CartService;
import com.duyell.communityfreshdelivery.service.DeliveryService;
import com.duyell.communityfreshdelivery.service.OrderService;
import com.duyell.communityfreshdelivery.service.PaymentService;
import com.duyell.communityfreshdelivery.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>评价 — 端到端验证</h2>
 *
 * <p>覆盖创建评价/重复评价/非本人订单/非可评价状态/订单级查询/商品级分页/我的评价.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@SpringBootTest
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CartService cartService;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private com.duyell.communityfreshdelivery.mapper.DeliveryMapper deliveryMapper;
    @Autowired
    private com.duyell.communityfreshdelivery.mapper.ProductSkuMapper productSkuMapper;

    private static final Long USER_ID = 1L;
    private static final Long MERCHANT_ID = 3L;
    private static final Long COURIER_ID = 2L;
    private static final Long SKU_ID = 1L;

    private Long addressId;
    private final List<Long> reviewIdsToClean = new ArrayList<>();
    private final List<Long> deliveryIdsToClean = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 重置库存
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.duyell.communityfreshdelivery.entity.ProductSku> resetWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        resetWrapper.eq(com.duyell.communityfreshdelivery.entity.ProductSku::getId, SKU_ID)
                .set(com.duyell.communityfreshdelivery.entity.ProductSku::getStock, 100);
        productSkuMapper.update(null, resetWrapper);

        loginAs(USER_ID);
        cartService.clear();

        // 清理当前用户的评价记录
        List<Review> existingReviews = reviewMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, USER_ID)
        );
        for (Review r : existingReviews) {
            reviewMapper.deleteById(r.getId());
        }

        // 创建收货地址
        com.duyell.communityfreshdelivery.entity.Address address =
                new com.duyell.communityfreshdelivery.entity.Address();
        address.setUserId(USER_ID);
        address.setContact("测试用户");
        address.setPhone("13800000001");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("测试小区1栋101");
        addressMapper.insert(address);
        addressId = address.getId();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        loginAs(USER_ID);
        cartService.clear();
        for (Long id : reviewIdsToClean) {
            if (id != null) {
                reviewMapper.deleteById(id);
            }
        }
        reviewIdsToClean.clear();
        for (Long id : deliveryIdsToClean) {
            if (id != null) {
                deliveryMapper.deleteById(id);
            }
        }
        deliveryIdsToClean.clear();
        if (addressId != null) {
            addressMapper.deleteById(addressId);
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== 工具方法 ====================

    private void loginAs(Long userId) {
        String role = userId.equals(MERCHANT_ID) ? "ROLE_MERCHANT"
                : userId.equals(COURIER_ID) ? "ROLE_DELIVERY"
                : "ROLE_USER";
        UserDetailsImpl principal = new UserDetailsImpl(userId, null, List.of(role), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
    }

    private void addToCart(Long skuId, int quantity) {
        CartAddDTO dto = new CartAddDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        cartService.add(dto);
    }

    /** 下单 → 支付 → 接单 → 分拣 → 配送(抢单+取货+送达) → 返回已签收订单 */
    private OrderVO prepareDeliveredOrder() {
        loginAs(USER_ID);
        addToCart(SKU_ID, 2);
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setAddressId(addressId);
        dto.setDeliveryType(1);
        OrderVO order = orderService.place(dto);
        paymentService.pay(order.getId());

        loginAs(MERCHANT_ID);
        orderService.accept(order.getId());
        orderService.sortComplete(order.getId());

        loginAs(COURIER_ID);
        var dv = deliveryService.grab(order.getId());
        deliveryIdsToClean.add(dv.getId());
        deliveryService.confirmPickup(order.getId());
        deliveryService.confirmDelivery(order.getId());

        // 验证订单已签收
        loginAs(USER_ID);
        OrderVO updated = orderService.getById(order.getId());
        assertEquals("已签收/已送达自提点", updated.getStatusText());

        return updated;
    }

    // ==================== 创建评价 ====================

    @Test
    void testCreateReview() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L); // 盒装草莓
        dto.setScore(5);
        dto.setContent("非常新鲜，包装完好");
        ReviewVO vo = reviewService.create(dto);
        reviewIdsToClean.add(vo.getId());

        assertNotNull(vo.getId());
        assertEquals(order.getId(), vo.getOrderId());
        assertEquals(5, vo.getScore());
        assertEquals("非常新鲜，包装完好", vo.getContent());
        assertNotNull(vo.getUserNickname());

        // 验证订单状态更新为已完成
        OrderVO updatedOrder = orderService.getById(order.getId());
        assertEquals("已完成", updatedOrder.getStatusText());

        System.out.println("\n=== 评价创建验证 ===");
        System.out.printf("评分=%d 内容=%s 用户=%s 订单状态=%s%n",
                vo.getScore(), vo.getContent(), vo.getUserNickname(), updatedOrder.getStatusText());
    }

    @Test
    void testCreateReviewWithoutContent() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(4);
        ReviewVO vo = reviewService.create(dto);
        reviewIdsToClean.add(vo.getId());

        assertEquals(4, vo.getScore());
        assertNotNull(vo.getProductName());
        System.out.println("\n=== 无文字评价验证 ===");
        System.out.printf("纯评分=%d 商品=%s%n", vo.getScore(), vo.getProductName());
    }

    // ==================== 重复评价 ====================

    @Test
    void testDuplicateReviewFails() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(5);
        ReviewVO first = reviewService.create(dto);
        reviewIdsToClean.add(first.getId());

        try {
            reviewService.create(dto);
            fail("重复评价应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("已评价过"));
        }
        System.out.println("\n=== 重复评价验证 ===");
        System.out.println("同一订单同一商品不可重复评价 ✓");
    }

    // ==================== 非本人订单 ====================

    @Test
    void testCannotReviewOthersOrder() {
        OrderVO order = prepareDeliveredOrder();

        // 以另一个用户身份尝试评价
        loginAs(COURIER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(1);

        try {
            reviewService.create(dto);
            fail("评价他人订单应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("订单不存在"));
        }
        System.out.println("\n=== 订单归属验证 ===");
        System.out.println("非本人订单不可评价 ✓");
    }

    // ==================== 状态校验 ====================

    @Test
    void testCannotReviewUnpaidOrder() {
        loginAs(USER_ID);
        addToCart(SKU_ID, 2);
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setAddressId(addressId);
        dto.setDeliveryType(1);
        OrderVO order = orderService.place(dto);
        // 未支付

        ReviewCreateDTO reviewDTO = new ReviewCreateDTO();
        reviewDTO.setOrderId(order.getId());
        reviewDTO.setProductId(1L);
        reviewDTO.setScore(3);

        try {
            reviewService.create(reviewDTO);
            fail("未完成订单不可评价");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("不可评价"));
        }
        System.out.println("\n=== 状态校验验证 ===");
        System.out.println("非已签收/已自提状态不可评价 ✓");
    }

    // ==================== 查询 ====================

    @Test
    void testGetByOrderId() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(5);
        dto.setContent("好评");
        ReviewVO vo = reviewService.create(dto);
        reviewIdsToClean.add(vo.getId());

        List<ReviewVO> reviews = reviewService.getByOrderId(order.getId());
        assertFalse(reviews.isEmpty());
        assertEquals(1, reviews.size());
        assertEquals("好评", reviews.get(0).getContent());

        System.out.println("\n=== 订单评价查询验证 ===");
        System.out.printf("订单=%d 评价数=%d%n", order.getId(), reviews.size());
    }

    @Test
    void testGetByProductId() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(4);
        reviewService.create(dto);
        reviewIdsToClean.add(0L); // placeholder, cleaned in tearDown

        Page<ReviewVO> page = reviewService.getByProductId(1L, 1, 10);
        assertTrue(page.getTotal() >= 1);
        ReviewVO first = page.getRecords().get(0);
        assertEquals(1L, first.getProductId());
        assertNotNull(first.getUserNickname());
        assertNotNull(first.getProductName());

        System.out.println("\n=== 商品评价列表验证 ===");
        System.out.printf("商品=%s 评价数=%d%n", first.getProductName(), page.getTotal());
    }

    @Test
    void testMyReviews() {
        OrderVO order = prepareDeliveredOrder();

        loginAs(USER_ID);
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setOrderId(order.getId());
        dto.setProductId(1L);
        dto.setScore(5);
        reviewService.create(dto);
        reviewIdsToClean.add(0L);

        Page<ReviewVO> page = reviewService.myReviews(1, 10);
        assertTrue(page.getTotal() >= 1);
        assertEquals(USER_ID, page.getRecords().get(0).getUserId());

        System.out.println("\n=== 我的评价验证 ===");
        System.out.printf("我的评价数=%d%n", page.getTotal());
    }

    // ==================== 空结果 ====================

    @Test
    void testNoReviewsForUnreviewedProduct() {
        Page<ReviewVO> page = reviewService.getByProductId(999L, 1, 10);
        assertEquals(0, page.getTotal());

        System.out.println("\n=== 空结果验证 ===");
        System.out.println("未评价商品返回空列表 ✓");
    }
}
