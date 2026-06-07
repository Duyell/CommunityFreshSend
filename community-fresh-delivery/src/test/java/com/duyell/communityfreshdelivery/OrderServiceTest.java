package com.duyell.communityfreshdelivery;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.entity.Address;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.duyell.communityfreshdelivery.service.CartService;
import com.duyell.communityfreshdelivery.service.OrderService;
import com.duyell.communityfreshdelivery.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>订单 — 端到端验证</h2>
 *
 * <p>覆盖下单/支付/取消支付/取消订单/详情/分页/用户隔离.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long SKU_ID = 1L;       // 盒装草莓 300g/盒 ¥19.90 stock=100
    private static final Long SKU_ID_2 = 2L;     // 盒装草莓 500g/盒 ¥29.90 stock=50

    private Long addressId;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(TEST_USER_ID, null, List.of("ROLE_USER"), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);

        cartService.clear();

        // 创建测试收货地址
        Address address = new Address();
        address.setUserId(TEST_USER_ID);
        address.setContact("测试用户");
        address.setPhone("13800000001");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("测试小区1栋101");
        addressMapper.insert(address);
        addressId = address.getId();
    }

    @AfterEach
    void tearDown() {
        cartService.clear();
        if (addressId != null) {
            addressMapper.deleteById(addressId);
        }
        SecurityContextHolder.clearContext();
    }

    private void addToCart(Long skuId, int quantity) {
        CartAddDTO dto = new CartAddDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        cartService.add(dto);
    }

    private OrderCreateDTO buildPlaceDTO() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setAddressId(addressId);
        dto.setDeliveryType(1);
        dto.setDeliveryTimeSlot("上午(9-12)");
        return dto;
    }

    // ==================== 下单 ====================

    @Test
    void testPlaceOrder() {
        addToCart(SKU_ID, 2);

        OrderVO order = orderService.place(buildPlaceDTO());

        assertNotNull(order.getId());
        assertNotNull(order.getOrderNo());
        assertEquals(0, order.getStatus());
        assertEquals("待付款", order.getStatusText());
        assertEquals(1, order.getDeliveryType());
        assertEquals(1, order.getItems().size());
        assertEquals(SKU_ID, order.getItems().get(0).getSkuId());
        assertEquals(2, order.getItems().get(0).getQuantity());

        // 购物车应已清空
        assertTrue(cartService.list().isEmpty());

        System.out.println("\n=== 下单验证 ===");
        System.out.printf("订单号=%s 状态=%s 实付=%.2f 件数=%d%n",
                order.getOrderNo(), order.getStatusText(),
                order.getActualAmount(), order.getItems().size());
    }

    @Test
    void testPlacePickupOrder() {
        addToCart(SKU_ID, 1);

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPickupPointId(1L);
        dto.setDeliveryType(2);

        OrderVO order = orderService.place(dto);

        assertNotNull(order.getPickupCode());
        assertEquals(6, order.getPickupCode().length());

        System.out.println("\n=== 自提下单验证 ===");
        System.out.printf("订单号=%s 自提码=%s%n", order.getOrderNo(), order.getPickupCode());
    }

    @Test
    void testPlaceOrderCartEmpty() {
        try {
            orderService.place(buildPlaceDTO());
            fail("购物车为空应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("购物车为空"));
        }
        System.out.println("\n=== 空购物车校验 ===");
        System.out.println("购物车为空，正确拒绝下单 ✓");
    }

    @Test
    void testPlaceOrderStockExceeded() {
        // SKU 1 库存 100，加购 200 件（超过库存）
        addToCart(SKU_ID, 200);

        try {
            orderService.place(buildPlaceDTO());
            fail("库存不足应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("库存不足"));
        }

        // 购物车应保持原样（事务回滚）
        assertFalse(cartService.list().isEmpty());

        System.out.println("\n=== 库存不足校验 ===");
        System.out.println("库存不足，正确拒绝下单 ✓");
    }

    @Test
    void testPlaceOrderBelowMinAmount() {
        // 1件 ¥19.90 < 起送价 ¥15.00? No, 19.90 > 15
        // 让我们用... wait, SKU 1 is 19.90 which is > 15
        // We need to test below min amount. Let me check - min is 15, SKU1 is 19.90
        // Actually there's no SKU below 15 in test data except SKU3 (5.90)
        // Let me add 1 of SKU3 which is 5.90
        addToCart(3L, 1);  // 有机生菜 ¥5.90 < ¥15.00

        try {
            orderService.place(buildPlaceDTO());
            fail("不满足起送价应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("起送"));
        }
        System.out.println("\n=== 起送价校验 ===");
        System.out.println("不满足起送价，正确拒绝下单 ✓");
    }

    // ==================== 支付 ====================

    @Test
    void testPayOrder() {
        addToCart(SKU_ID, 2);
        OrderVO order = orderService.place(buildPlaceDTO());

        paymentService.pay(order.getId());

        OrderVO paid = orderService.getById(order.getId());
        assertEquals(1, paid.getStatus());
        assertEquals("待接单", paid.getStatusText());
        assertNotNull(paid.getPaidTime());

        System.out.println("\n=== 支付验证 ===");
        System.out.printf("订单号=%s 支付后状态=%s 支付时间=%s%n",
                paid.getOrderNo(), paid.getStatusText(), paid.getPaidTime());
    }

    @Test
    void testPayNonPendingOrder() {
        addToCart(SKU_ID, 2);
        OrderVO order = orderService.place(buildPlaceDTO());
        paymentService.pay(order.getId());

        try {
            paymentService.pay(order.getId());
            fail("重复支付应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("不可支付"));
        }
        System.out.println("\n=== 重复支付校验 ===");
        System.out.println("已支付订单拒绝再次支付 ✓");
    }

    @Test
    void testCancelPay() {
        Integer originalStock = productSkuMapper.selectById(SKU_ID).getStock();

        addToCart(SKU_ID, 2);
        OrderVO order = orderService.place(buildPlaceDTO());

        paymentService.cancelPay(order.getId());

        OrderVO cancelled = orderService.getById(order.getId());
        assertEquals(9, cancelled.getStatus());
        assertEquals("已取消", cancelled.getStatusText());
        assertTrue(cancelled.getCancelReason().contains("取消支付"));

        // 库存应恢复到原始值
        Integer stockAfter = productSkuMapper.selectById(SKU_ID).getStock();
        assertEquals(originalStock, stockAfter);

        System.out.println("\n=== 取消支付验证 ===");
        System.out.printf("订单状态=%s 库存: %d→%d (一致)%n",
                cancelled.getStatusText(), originalStock, stockAfter);
    }

    // ==================== 取消订单 ====================

    @Test
    void testCancelOrder() {
        Integer originalStock = productSkuMapper.selectById(SKU_ID).getStock();

        addToCart(SKU_ID, 3);
        OrderVO order = orderService.place(buildPlaceDTO());
        paymentService.pay(order.getId());

        orderService.cancel(order.getId(), "不想要了");

        OrderVO cancelled = orderService.getById(order.getId());
        assertEquals(9, cancelled.getStatus());
        assertEquals("不想要了", cancelled.getCancelReason());

        // 库存应恢复到原始值
        Integer stockAfter = productSkuMapper.selectById(SKU_ID).getStock();
        assertEquals(originalStock, stockAfter);

        System.out.println("\n=== 取消订单验证 ===");
        System.out.printf("订单状态=%s 原因=%s 库存已回补%n",
                cancelled.getStatusText(), cancelled.getCancelReason());
    }

    // ==================== 订单详情 ====================

    @Test
    void testGetOrderDetail() {
        addToCart(SKU_ID, 1);
        addToCart(SKU_ID_2, 1);
        OrderVO placed = orderService.place(buildPlaceDTO());

        OrderVO detail = orderService.getById(placed.getId());

        assertEquals(placed.getId(), detail.getId());
        assertEquals(2, detail.getItems().size());

        // 验证明细有商品信息快照
        detail.getItems().forEach(i -> {
            assertNotNull(i.getProductName());
            assertNotNull(i.getSpecName());
            assertNotNull(i.getPrice());
            assertTrue(i.getQuantity() > 0);
        });

        System.out.println("\n=== 订单详情验证 ===");
        detail.getItems().forEach(i ->
                System.out.printf("  %s/%s 数量=%d 单价=%.2f%n",
                        i.getProductName(), i.getSpecName(),
                        i.getQuantity(), i.getPrice()));
    }

    // ==================== 用户隔离 ====================

    @Test
    void testUserIsolation() {
        addToCart(SKU_ID, 1);
        OrderVO order = orderService.place(buildPlaceDTO());

        // 切换为另一个用户
        UserDetailsImpl other = new UserDetailsImpl(2L, null, List.of("ROLE_USER"), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(other, null, other.getAuthorities())
        );
        SecurityContextHolder.setContext(context);

        try {
            orderService.getById(order.getId());
            fail("其他用户不应看到我的订单");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("订单不存在"));
        }

        System.out.println("\n=== 用户隔离验证 ===");
        System.out.println("用户 " + TEST_USER_ID + " 的订单对用户 2 不可见 ✓");
    }

    // ==================== 分页 ====================

    @Test
    void testOrderPage() {
        addToCart(SKU_ID, 1);
        orderService.place(buildPlaceDTO());

        addToCart(SKU_ID_2, 1);
        orderService.place(buildPlaceDTO());

        Page<OrderVO> page = orderService.page(1, 10, null);
        assertTrue(page.getTotal() >= 2);

        // 按状态筛选
        Page<OrderVO> pendingPage = orderService.page(1, 10, 0);
        assertTrue(pendingPage.getTotal() >= 2);

        System.out.println("\n=== 订单分页验证 ===");
        System.out.printf("全部=%d 待付款=%d%n", page.getTotal(), pendingPage.getTotal());
    }
}
