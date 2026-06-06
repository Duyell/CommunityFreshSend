package com.duyell.communityfreshdelivery;

import com.duyell.communityfreshdelivery.dto.ProductVO;
import com.duyell.communityfreshdelivery.service.ProductService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>用户端商品接口 — 端到端验证</h2>
 *
 * <p>覆盖 3.3a（分类浏览）+ 3.3b（关键词搜索），验证公开接口的数据正确性.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@SpringBootTest
class ProductBrowseTest {

    @Autowired
    private ProductService productService;

    @Test
    void testListAllProducts() {
        // 全部分类，默认按时间排序
        Page<ProductVO> result = productService.listForUser(1, 10, null, null, "time");
        System.out.println("\n=== 全部分类浏览（默认时间排序）===");
        System.out.println("总数=" + result.getTotal());
        result.getRecords().forEach(p ->
                System.out.printf("  [%d] %s 最低价=%.2f 状态=%d%n",
                        p.getId(), p.getName(), p.getMinPrice(), p.getStatus()));

        assertTrue(result.getTotal() >= 4, "至少应有 4 个上架商品");
        // 验证全部是上架状态
        result.getRecords().forEach(p ->
                assertEquals(1, p.getStatus(), "用户端只能看到上架商品: " + p.getName()));
    }

    @Test
    void testListByCategory() {
        // 分类 2 = 水果 → 盒装草莓 + 菲律宾菠萝
        Page<ProductVO> result = productService.listForUser(1, 10, 2L, null, "time");
        System.out.println("\n=== 分类 2（水果）浏览 ===");
        result.getRecords().forEach(p ->
                System.out.printf("  [%d] %s 最低价=%.2f%n", p.getId(), p.getName(), p.getMinPrice()));

        assertEquals(2, result.getTotal(), "分类 2 应有 2 个商品");
        assertTrue(result.getRecords().stream().anyMatch(p -> "盒装草莓".equals(p.getName())));
        assertTrue(result.getRecords().stream().anyMatch(p -> "菲律宾菠萝".equals(p.getName())));
    }

    @Test
    void testListSortedByPriceAsc() {
        // 价格升序
        Page<ProductVO> result = productService.listForUser(1, 10, null, null, "price_asc");
        System.out.println("\n=== 全部分类（价格升序）===");
        result.getRecords().forEach(p ->
                System.out.printf("  [%d] %s 最低价=%.2f%n", p.getId(), p.getName(), p.getMinPrice()));

        // 验证升序：第一个应该是最便宜的
        var prices = result.getRecords().stream()
                .map(ProductVO::getMinPrice)
                .filter(p -> p != null)
                .toList();
        for (int i = 1; i < prices.size(); i++) {
            assertTrue(prices.get(i - 1).compareTo(prices.get(i)) <= 0,
                    "价格应按升序排列");
        }
    }

    @Test
    void testListSortedByPriceDesc() {
        // 价格降序
        Page<ProductVO> result = productService.listForUser(1, 10, null, null, "price_desc");
        System.out.println("\n=== 全部分类（价格降序）===");
        result.getRecords().forEach(p ->
                System.out.printf("  [%d] %s 最低价=%.2f%n", p.getId(), p.getName(), p.getMinPrice()));

        var prices = result.getRecords().stream()
                .map(ProductVO::getMinPrice)
                .filter(p -> p != null)
                .toList();
        for (int i = 1; i < prices.size(); i++) {
            assertTrue(prices.get(i - 1).compareTo(prices.get(i)) >= 0,
                    "价格应按降序排列");
        }
    }

    @Test
    void testSearchByKeyword() {
        // 搜索"肉"
        Page<ProductVO> result = productService.listForUser(1, 10, null, "肉", "time");
        System.out.println("\n=== 搜索'肉' ===");
        result.getRecords().forEach(p ->
                System.out.printf("  [%d] %s 最低价=%.2f%n", p.getId(), p.getName(), p.getMinPrice()));

        assertTrue(result.getTotal() >= 1, "至少搜到 1 个含'肉'的商品");
        assertTrue(result.getRecords().stream().anyMatch(p -> p.getName().contains("肉")),
                "搜索结果应包含'五花肉'");
    }

    @Test
    void testSearchWithNoResult() {
        // 搜索不存在的商品
        Page<ProductVO> result = productService.listForUser(1, 10, null, "不存在的关键词XYZ", "time");
        System.out.println("\n=== 搜索'不存在XYZ' ===");
        System.out.println("总数=" + result.getTotal());

        assertEquals(0, result.getTotal(), "不存在的关键词应返回空");
    }

    @Test
    void testListPagination() {
        // 每页 2 条，验证分页正确
        Page<ProductVO> page1 = productService.listForUser(1, 2, null, null, "time");
        Page<ProductVO> page2 = productService.listForUser(2, 2, null, null, "time");

        System.out.println("\n=== 分页验证（每页2条）===");
        System.out.println("总条数=" + page1.getTotal());
        System.out.printf("第1页: %d 条 (共 %d 页)%n", page1.getRecords().size(), page1.getPages());
        page1.getRecords().forEach(p -> System.out.printf("  [%d] %s%n", p.getId(), p.getName()));
        System.out.printf("第2页: %d 条%n", page2.getRecords().size());
        page2.getRecords().forEach(p -> System.out.printf("  [%d] %s%n", p.getId(), p.getName()));

        assertEquals(2, page1.getRecords().size(), "第1页应有2条");
        // 第1页和第2页不应有重复
        var page1Ids = page1.getRecords().stream().map(ProductVO::getId).toList();
        var page2Ids = page2.getRecords().stream().map(ProductVO::getId).toList();
        assertTrue(page1Ids.stream().noneMatch(page2Ids::contains), "两页不应有重复商品");
    }

    @Test
    void testListContainsRequiredFields() {
        // 用户端列表应包含最低售价和基本信息（SKU 列表由 toVO 统一填充，与商家端一致）
        Page<ProductVO> result = productService.listForUser(1, 10, null, null, "time");
        result.getRecords().forEach(p -> {
            assertNotNull(p.getId(), "商品 ID 不能为空");
            assertNotNull(p.getName(), "商品名称不能为空");
            assertNotNull(p.getMinPrice(), "列表应包含最低售价: " + p.getName());
            assertEquals(1, p.getStatus(), "仅返回上架商品");
        });
        System.out.println("\n=== 列表字段验证 ===");
        System.out.println("全部商品字段完整，status=1，最低售价已计算 ✓");
    }
}
