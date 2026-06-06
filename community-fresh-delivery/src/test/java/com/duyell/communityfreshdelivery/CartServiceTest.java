package com.duyell.communityfreshdelivery;

import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.CartItemVO;
import com.duyell.communityfreshdelivery.service.CartService;
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
 * <h2>购物车 — 端到端验证</h2>
 *
 * <p>覆盖加购/累加/改数量/删单品/清空/列表.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@SpringBootTest
class CartServiceTest {

    @Autowired
    private CartService cartService;

    private static final Long TEST_USER_ID = 99L;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(TEST_USER_ID, null, List.of("ROLE_USER"), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
        cartService.clear();
    }

    @AfterEach
    void tearDown() {
        cartService.clear();
        SecurityContextHolder.clearContext();
    }

    private CartAddDTO buildAddDTO(Long skuId, int qty) {
        CartAddDTO dto = new CartAddDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(qty);
        return dto;
    }

    @Test
    void testAddAndList() {
        cartService.add(buildAddDTO(1L, 3));

        List<CartItemVO> items = cartService.list();
        assertEquals(1, items.size());
        CartItemVO item = items.get(0);
        assertEquals(1L, item.getSkuId());
        assertEquals(3, item.getQuantity());
        assertEquals("300g/盒", item.getSpecName());
        assertNotNull(item.getProductName());
        assertNotNull(item.getPrice());
        assertTrue(item.getStockSufficient());

        System.out.println("\n=== 加购验证 ===");
        System.out.printf("商品=%s 规格=%s 单价=%.2f 数量=%d 库存充足=%s%n",
                item.getProductName(), item.getSpecName(), item.getPrice(),
                item.getQuantity(), item.getStockSufficient());
    }

    @Test
    void testAddAccumulatesQuantity() {
        cartService.add(buildAddDTO(1L, 2));
        cartService.add(buildAddDTO(1L, 3));

        List<CartItemVO> items = cartService.list();
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getQuantity(), "同 SKU 两次加购应累加数量");

        System.out.println("\n=== 累加验证 ===");
        System.out.printf("skuId=1 数量=%d（预期 2+3=5）%n", items.get(0).getQuantity());
    }

    @Test
    void testUpdateQty() {
        cartService.add(buildAddDTO(1L, 2));
        cartService.updateQty(buildAddDTO(1L, 5));

        List<CartItemVO> items = cartService.list();
        assertEquals(5, items.get(0).getQuantity(), "修改数量直接覆盖");

        System.out.println("\n=== 修改数量验证 ===");
        System.out.printf("skuId=1 数量=%d（预期 5）%n", items.get(0).getQuantity());
    }

    @Test
    void testRemove() {
        cartService.add(buildAddDTO(1L, 2));
        cartService.add(buildAddDTO(2L, 3));
        assertEquals(2, cartService.list().size());

        cartService.remove(1L);
        List<CartItemVO> items = cartService.list();
        assertEquals(1, items.size());
        assertEquals(2L, items.get(0).getSkuId());

        System.out.println("\n=== 删除单品验证 ===");
        System.out.printf("删除 skuId=1 后剩余 skuId=%d%n", items.get(0).getSkuId());
    }

    @Test
    void testClear() {
        cartService.add(buildAddDTO(1L, 1));
        cartService.add(buildAddDTO(2L, 1));
        cartService.clear();

        List<CartItemVO> items = cartService.list();
        assertTrue(items.isEmpty(), "清空后列表应为空");

        System.out.println("\n=== 清空验证 ===");
        System.out.println("清空后购物车为空 ✓");
    }

    @Test
    void testMultipleItems() {
        cartService.add(buildAddDTO(1L, 2));
        cartService.add(buildAddDTO(3L, 1));
        cartService.add(buildAddDTO(4L, 3));

        List<CartItemVO> items = cartService.list();
        assertEquals(3, items.size());

        System.out.println("\n=== 多商品验证 ===");
        items.forEach(i -> System.out.printf("  skuId=%d %s/%s 数量=%d%n",
                i.getSkuId(), i.getProductName(), i.getSpecName(), i.getQuantity()));
    }

    @Test
    void testEmptyCart() {
        List<CartItemVO> items = cartService.list();
        assertTrue(items.isEmpty());
        System.out.println("\n=== 空购物车验证 ===");
        System.out.println("首次访问，购物车为空 ✓");
    }

    @Test
    void testUserIsolation() {
        // 当前用户 99 加购
        cartService.add(buildAddDTO(1L, 5));

        // 切换用户
        UserDetailsImpl other = new UserDetailsImpl(100L, null, List.of("ROLE_USER"), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(other, null, other.getAuthorities())
        );
        SecurityContextHolder.setContext(context);

        List<CartItemVO> otherItems = cartService.list();
        assertTrue(otherItems.isEmpty(), "用户 100 不应看到用户 99 的购物车");

        System.out.println("\n=== 用户隔离验证 ===");
        System.out.println("用户 99 有数据，用户 100 为空 ✓");
    }
}
