package com.duyell.communityfreshdelivery;

import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplyDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderReviewDTO;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.mapper.GroupLeaderApplicationMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import com.duyell.communityfreshdelivery.service.CartService;
import com.duyell.communityfreshdelivery.service.DeliveryService;
import com.duyell.communityfreshdelivery.service.GroupLeaderApplicationService;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>提货核销 — 端到端验证</h2>
 *
 * <p>覆盖提货码核销/错误提货码/非团长核销/非本人自提点/状态校验.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@SpringBootTest
class PickupVerifyServiceTest {

    @Autowired
    private GroupLeaderApplicationService applicationService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CartService cartService;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private GroupLeaderApplicationMapper applicationMapper;
    @Autowired
    private PickupPointMapper pickupPointMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private com.duyell.communityfreshdelivery.mapper.DeliveryMapper deliveryMapper;
    @Autowired
    private com.duyell.communityfreshdelivery.mapper.ProductSkuMapper productSkuMapper;
    @Autowired
    private com.duyell.communityfreshdelivery.mapper.AddressMapper addressMapper;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 4L;
    private static final Long MERCHANT_ID = 3L;
    private static final Long COURIER_ID = 2L;
    private static final Long SKU_ID = 1L;

    private Long myPickupPointId;
    private final List<Long> applicationIdsToClean = new ArrayList<>();
    private final List<Long> pickupPointIdsToClean = new ArrayList<>();
    private final List<Long> deliveryIdsToClean = new ArrayList<>();
    private Long addressId;

    @BeforeEach
    void setUp() {
        // 物理清除 user 1 的 ROLE_GROUP_LEADER（防 @TableLogic 软删除残留导致唯一约束冲突）
        userRoleMapper.physicalDelete(USER_ID, "ROLE_GROUP_LEADER");

        // 重置库存
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.duyell.communityfreshdelivery.entity.ProductSku> resetWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        resetWrapper.eq(com.duyell.communityfreshdelivery.entity.ProductSku::getId, SKU_ID)
                .set(com.duyell.communityfreshdelivery.entity.ProductSku::getStock, 100);
        productSkuMapper.update(null, resetWrapper);

        // 清理旧数据
        loginAs(USER_ID);
        cartService.clear();
        List<com.duyell.communityfreshdelivery.entity.GroupLeaderApplication> existing =
                applicationMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                com.duyell.communityfreshdelivery.entity.GroupLeaderApplication>()
                                .eq(com.duyell.communityfreshdelivery.entity.GroupLeaderApplication::getUserId, USER_ID)
                );
        for (var app : existing) {
            applicationMapper.deleteById(app.getId());
        }

        // 创建收货地址（防止下单校验需要）
        loginAs(USER_ID);
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

        // 1. 用户提交团长申请
        loginAs(USER_ID);
        GroupLeaderApplyDTO applyDTO = new GroupLeaderApplyDTO();
        applyDTO.setAddress("阳光花园东门101室");
        applyDTO.setContactName("张三");
        applyDTO.setContactPhone("13800001111");
        var app = applicationService.apply(applyDTO);
        applicationIdsToClean.add(app.getId());

        // 2. 管理员审核通过 → 创建自提点 + 追加角色
        loginAs(ADMIN_ID);
        GroupLeaderReviewDTO review = new GroupLeaderReviewDTO();
        review.setApproved(true);
        applicationService.review(app.getId(), review);

        // 获取创建的自提点
        var points = pickupPointMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.duyell.communityfreshdelivery.entity.PickupPoint>()
                        .eq(com.duyell.communityfreshdelivery.entity.PickupPoint::getOwnerType, 2)
                        .eq(com.duyell.communityfreshdelivery.entity.PickupPoint::getOwnerId, USER_ID)
        );
        if (!points.isEmpty()) {
            myPickupPointId = points.get(0).getId();
            pickupPointIdsToClean.add(myPickupPointId);
        }
    }

    @AfterEach
    void tearDown() {
        loginAs(USER_ID);
        cartService.clear();

        for (Long id : deliveryIdsToClean) {
            if (id != null) {
                deliveryMapper.deleteById(id);
            }
        }
        deliveryIdsToClean.clear();
        for (Long id : applicationIdsToClean) {
            if (id != null) {
                applicationMapper.deleteById(id);
            }
        }
        applicationIdsToClean.clear();
        for (Long id : pickupPointIdsToClean) {
            if (id != null) {
                pickupPointMapper.deleteById(id);
            }
        }
        pickupPointIdsToClean.clear();
        // 物理清理追加的 ROLE_GROUP_LEADER
        userRoleMapper.physicalDelete(USER_ID, "ROLE_GROUP_LEADER");
        if (addressId != null) {
            addressMapper.deleteById(addressId);
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== 工具方法 ====================

    private void loginAs(Long userId) {
        String role = userId.equals(ADMIN_ID) ? "ROLE_ADMIN"
                : userId.equals(MERCHANT_ID) ? "ROLE_MERCHANT"
                : userId.equals(COURIER_ID) ? "ROLE_DELIVERY"
                : "ROLE_USER";
        // 团长角色追加后仍需在主测试中单独设置
        UserDetailsImpl principal = new UserDetailsImpl(userId, null, List.of(role), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
    }

    /** 以团长身份登录（ROLE_GROUP_LEADER） */
    private void loginAsGroupLeader(Long userId) {
        UserDetailsImpl principal = new UserDetailsImpl(userId, null, List.of("ROLE_GROUP_LEADER"), true);
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

    /** 下自提单 → 支付 → 接单 → 分拣 → 配送接单+取货+送达 → 返回订单 */
    private OrderVO prepareOrderAtPickupPoint() {
        assertNotNull(myPickupPointId);

        // 用户下单（self-pickup）
        loginAs(USER_ID);
        cartService.clear();
        addToCart(SKU_ID, 2);
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPickupPointId(myPickupPointId);
        dto.setDeliveryType(2);
        dto.setDeliveryTimeSlot("上午(9-12)");
        OrderVO order = orderService.place(dto);

        // 验证自提码已生成
        assertNotNull(order.getPickupCode());
        assertFalse(order.getPickupCode().isEmpty());

        // 支付
        paymentService.pay(order.getId());

        // 商家接单+分拣
        loginAs(MERCHANT_ID);
        orderService.accept(order.getId());
        orderService.sortComplete(order.getId());

        // 配送员抢单+取货+送达
        loginAs(COURIER_ID);
        var dv = deliveryService.grab(order.getId());
        deliveryIdsToClean.add(dv.getId());
        deliveryService.confirmPickup(order.getId());
        deliveryService.confirmDelivery(order.getId());

        // 验证订单状态为"已送达自提点"
        loginAs(USER_ID);
        OrderVO updated = orderService.getById(order.getId());
        assertEquals("已签收/已送达自提点", updated.getStatusText());

        return updated;
    }

    // ==================== 核销成功 ====================

    @Test
    void testVerifyPickup() {
        OrderVO order = prepareOrderAtPickupPoint();
        String pickupCode = order.getPickupCode();
        assertNotNull(pickupCode);

        // 团长核销
        loginAsGroupLeader(USER_ID);
        applicationService.verifyPickup(pickupCode);

        // 验证订单状态变更
        loginAs(USER_ID);
        OrderVO updated = orderService.getById(order.getId());
        assertEquals("用户已自提", updated.getStatusText());

        System.out.println("\n=== 提货核销验证 ===");
        System.out.printf("提货码=%s 订单状态=%s%n", pickupCode, updated.getStatusText());
    }

    // ==================== 错误提货码 ====================

    @Test
    void testVerifyWithWrongCodeFails() {
        prepareOrderAtPickupPoint();

        loginAsGroupLeader(USER_ID);
        try {
            applicationService.verifyPickup("000000");
            fail("错误提货码应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("提货码无效"));
        }
        System.out.println("\n=== 错误提货码验证 ===");
        System.out.println("无效提货码被拒绝 ✓");
    }

    // ==================== 非团长核销 ====================

    @Test
    void testNonGroupLeaderCannotVerify() {
        OrderVO order = prepareOrderAtPickupPoint();

        // 以无自提点的用户身份尝试核销
        // COURIER_ID=2 有 ROLE_DELIVERY 但无团长自提点
        loginAs(COURIER_ID);
        try {
            applicationService.verifyPickup(order.getPickupCode());
            fail("非团长核销应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("不是团长"));
        }
        System.out.println("\n=== 非团长核销验证 ===");
        System.out.println("无自提点用户无权核销 ✓");
    }

    // ==================== 非本人自提点 ====================

    @Test
    void testCannotVerifyOtherPickupPoint() {
        OrderVO order = prepareOrderAtPickupPoint();

        // 以另一个用户（COURIER_ID=2）模拟——他没有团长自提点
        // 但先给他创建一个不同的自提点（直接插入 DB）
        loginAs(COURIER_ID);
        com.duyell.communityfreshdelivery.entity.PickupPoint otherPoint =
                new com.duyell.communityfreshdelivery.entity.PickupPoint();
        otherPoint.setName("另一个自提点");
        otherPoint.setContact("李四");
        otherPoint.setPhone("13800002222");
        otherPoint.setAddress("隔壁小区西门202");
        otherPoint.setOwnerType(2);
        otherPoint.setOwnerId(COURIER_ID);
        otherPoint.setStatus(1);
        pickupPointMapper.insert(otherPoint);
        pickupPointIdsToClean.add(otherPoint.getId());

        // 给 COURIER_ID 追加 ROLE_GROUP_LEADER
        com.duyell.communityfreshdelivery.entity.UserRole role =
                new com.duyell.communityfreshdelivery.entity.UserRole();
        role.setUserId(COURIER_ID);
        role.setRole("ROLE_GROUP_LEADER");
        userRoleMapper.insert(role);

        // 以 COURIER_ID 团长身份核销（订单属于 USER_ID 的自提点）
        loginAsGroupLeader(COURIER_ID);
        try {
            applicationService.verifyPickup(order.getPickupCode());
            fail("核销他人自提点订单应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("不属于您的自提点"));
        }

        // 物理清理追加的角色
        userRoleMapper.physicalDelete(COURIER_ID, "ROLE_GROUP_LEADER");

        System.out.println("\n=== 跨自提点核销验证 ===");
        System.out.println("核销非本人自提点订单被拒绝 ✓");
    }

    // ==================== 状态校验 ====================

    @Test
    void testCannotVerifyNonReceivedOrder() {
        // 下单但不走完成流程——订单还在待付款状态
        loginAs(USER_ID);
        cartService.clear();
        addToCart(SKU_ID, 2);
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPickupPointId(myPickupPointId);
        dto.setDeliveryType(2);
        OrderVO order = orderService.place(dto);
        assertNotNull(order.getPickupCode());

        loginAsGroupLeader(USER_ID);
        try {
            applicationService.verifyPickup(order.getPickupCode());
            fail("未送达的订单不应可核销");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("尚未到达自提点"));
        }
        System.out.println("\n=== 状态校验验证 ===");
        System.out.println("未送达自提点的订单不可核销 ✓");
    }

    // ==================== 核销后不可重复 ====================

    @Test
    void testCannotVerifyTwice() {
        OrderVO order = prepareOrderAtPickupPoint();

        // 第一次核销
        loginAsGroupLeader(USER_ID);
        applicationService.verifyPickup(order.getPickupCode());

        // 第二次核销——状态已变为6（用户已自提），不再是5
        try {
            applicationService.verifyPickup(order.getPickupCode());
            fail("重复核销应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("尚未到达自提点"));
        }
        System.out.println("\n=== 重复核销验证 ===");
        System.out.println("已自提的订单不可重复核销 ✓");
    }
}
