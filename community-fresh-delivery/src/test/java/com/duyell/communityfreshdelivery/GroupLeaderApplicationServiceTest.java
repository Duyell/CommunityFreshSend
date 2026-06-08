package com.duyell.communityfreshdelivery;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplyDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplicationVO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderReviewDTO;
import com.duyell.communityfreshdelivery.mapper.GroupLeaderApplicationMapper;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import com.duyell.communityfreshdelivery.service.GroupLeaderApplicationService;
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
 * <h2>团长申请 — 端到端验证</h2>
 *
 * <p>覆盖提交申请/查看状态/管理员审核通过与拒绝/角色隔离.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@SpringBootTest
class GroupLeaderApplicationServiceTest {

    @Autowired
    private GroupLeaderApplicationService applicationService;
    @Autowired
    private GroupLeaderApplicationMapper applicationMapper;
    @Autowired
    private PickupPointMapper pickupPointMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 4L;

    private final List<Long> applicationIdsToClean = new ArrayList<>();
    private final List<Long> pickupPointIdsToClean = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 物理清除 user 1 的 ROLE_GROUP_LEADER（防 @TableLogic 软删除残留导致唯一约束冲突）
        userRoleMapper.physicalDelete(USER_ID, "ROLE_GROUP_LEADER");

        // 清理 user 1 已有的待审核申请
        List<com.duyell.communityfreshdelivery.entity.GroupLeaderApplication> existing =
                applicationMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                com.duyell.communityfreshdelivery.entity.GroupLeaderApplication>()
                                .eq(com.duyell.communityfreshdelivery.entity.GroupLeaderApplication::getUserId, USER_ID)
                );
        for (var app : existing) {
            applicationMapper.deleteById(app.getId());
        }
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
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
        SecurityContextHolder.clearContext();
    }

    // ==================== 工具方法 ====================

    private void loginAs(Long userId) {
        String role = userId.equals(ADMIN_ID) ? "ROLE_ADMIN" : "ROLE_USER";
        UserDetailsImpl principal = new UserDetailsImpl(userId, null, List.of(role), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
    }

    private GroupLeaderApplyDTO buildApplyDTO() {
        GroupLeaderApplyDTO dto = new GroupLeaderApplyDTO();
        dto.setAddress("阳光花园东门101室");
        dto.setContactName("张三");
        dto.setContactPhone("13800001111");
        return dto;
    }

    // ==================== 提交申请 ====================

    @Test
    void testApply() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO vo = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(vo.getId());

        assertNotNull(vo.getId());
        assertEquals(0, vo.getStatus());
        assertEquals("待审核", vo.getStatusText());
        assertEquals("阳光花园东门101室", vo.getAddress());
        assertEquals("张三", vo.getContactName());

        System.out.println("\n=== 团长申请提交验证 ===");
        System.out.printf("申请ID=%d 状态=%s 地址=%s%n", vo.getId(), vo.getStatusText(), vo.getAddress());
    }

    @Test
    void testDuplicateApplyFails() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO first = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(first.getId());

        // 重复提交
        try {
            applicationService.apply(buildApplyDTO());
            fail("重复申请应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("已有待审核"));
        }
        System.out.println("\n=== 重复申请验证 ===");
        System.out.println("重复申请正确拒绝 ✓");
    }

    // ==================== 查看申请 ====================

    @Test
    void testMyApplication() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO vo = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(vo.getId());

        GroupLeaderApplicationVO my = applicationService.myApplication();
        assertEquals(vo.getId(), my.getId());
        assertEquals("待审核", my.getStatusText());
        assertEquals("阳光花园东门101室", my.getAddress());

        System.out.println("\n=== 查看我的申请验证 ===");
        System.out.printf("申请ID=%d 状态=%s%n", my.getId(), my.getStatusText());
    }

    @Test
    void testMyApplicationWhenNoneExists() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO my = applicationService.myApplication();
        assertEquals("未申请", my.getStatusText());

        System.out.println("\n=== 未申请时验证 ===");
        System.out.println("未申请返回空 VO ✓");
    }

    // ==================== 管理员审核 ====================

    @Test
    void testApprove() {
        // 用户提交
        loginAs(USER_ID);
        GroupLeaderApplicationVO app = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(app.getId());

        // 管理员通过
        loginAs(ADMIN_ID);
        GroupLeaderReviewDTO review = new GroupLeaderReviewDTO();
        review.setApproved(true);
        GroupLeaderApplicationVO result = applicationService.review(app.getId(), review);

        assertEquals(1, result.getStatus());
        assertEquals("已通过", result.getStatusText());
        assertNotNull(result.getReviewedTime());

        // 验证 pickup_point 已创建
        var points = pickupPointMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.duyell.communityfreshdelivery.entity.PickupPoint>()
                        .eq(com.duyell.communityfreshdelivery.entity.PickupPoint::getOwnerType, 2)
                        .eq(com.duyell.communityfreshdelivery.entity.PickupPoint::getOwnerId, app.getUserId())
        );
        assertFalse(points.isEmpty());
        assertEquals("阳光花园东门101室", points.get(0).getAddress());
        pickupPointIdsToClean.add(points.get(0).getId());

        // 验证角色已追加
        boolean hasRole = userRoleMapper.exists(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.duyell.communityfreshdelivery.entity.UserRole>()
                        .eq(com.duyell.communityfreshdelivery.entity.UserRole::getUserId, app.getUserId())
                        .eq(com.duyell.communityfreshdelivery.entity.UserRole::getRole, "ROLE_GROUP_LEADER")
        );
        assertTrue(hasRole);

        System.out.println("\n=== 审核通过验证 ===");
        System.out.printf("申请状态=%s pickupPointId=%d 角色已追加%n",
                result.getStatusText(), points.get(0).getId());
    }

    @Test
    void testReject() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO app = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(app.getId());

        loginAs(ADMIN_ID);
        GroupLeaderReviewDTO review = new GroupLeaderReviewDTO();
        review.setApproved(false);
        review.setRejectReason("地址信息不完整");
        GroupLeaderApplicationVO result = applicationService.review(app.getId(), review);

        assertEquals(2, result.getStatus());
        assertEquals("已拒绝", result.getStatusText());
        assertEquals("地址信息不完整", result.getRejectReason());
        assertNotNull(result.getReviewedTime());

        System.out.println("\n=== 审核拒绝验证 ===");
        System.out.printf("状态=%s 原因=%s%n", result.getStatusText(), result.getRejectReason());
    }

    @Test
    void testRejectWithoutReasonFails() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO app = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(app.getId());

        loginAs(ADMIN_ID);
        GroupLeaderReviewDTO review = new GroupLeaderReviewDTO();
        review.setApproved(false);
        // 不填拒绝原因

        try {
            applicationService.review(app.getId(), review);
            fail("拒绝时无原因应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("拒绝原因不能为空"));
        }
        System.out.println("\n=== 拒绝原因校验 ===");
        System.out.println("无原因的拒绝被拦截 ✓");
    }

    @Test
    void testDuplicateReviewFails() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO app = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(app.getId());

        loginAs(ADMIN_ID);
        GroupLeaderReviewDTO review = new GroupLeaderReviewDTO();
        review.setApproved(false);
        review.setRejectReason("信息不全");
        applicationService.review(app.getId(), review);

        // 重复审核
        try {
            applicationService.review(app.getId(), review);
            fail("重复审核应抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("已审核"));
        }
        System.out.println("\n=== 重复审核验证 ===");
        System.out.println("已审核的申请不可重复操作 ✓");
    }

    // ==================== 管理员分页 ====================

    @Test
    void testPageApplications() {
        loginAs(USER_ID);
        GroupLeaderApplicationVO app = applicationService.apply(buildApplyDTO());
        applicationIdsToClean.add(app.getId());

        loginAs(ADMIN_ID);
        Page<GroupLeaderApplicationVO> page = applicationService.pageApplications(1, 10, null);
        assertTrue(page.getTotal() >= 1);

        // 按待审核筛选
        Page<GroupLeaderApplicationVO> pendingPage = applicationService.pageApplications(1, 10, 0);
        assertTrue(pendingPage.getTotal() >= 1);
        pendingPage.getRecords().forEach(v -> assertEquals(0, v.getStatus()));

        System.out.println("\n=== 管理分页验证 ===");
        System.out.printf("全部=%d 待审核=%d%n", page.getTotal(), pendingPage.getTotal());
    }
}
