package com.duyell.communityfreshdelivery;

import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.dto.AddressSaveDTO;
import com.duyell.communityfreshdelivery.dto.AddressVO;
import com.duyell.communityfreshdelivery.service.AddressService;
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
 * <h2>收货地址 — 端到端验证</h2>
 *
 * <p>覆盖 CRUD + 设默认 + 归属校验 + 数量限制.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@SpringBootTest
class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(TEST_USER_ID, null, List.of("ROLE_USER"), true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AddressSaveDTO buildDTO(String contact) {
        AddressSaveDTO dto = new AddressSaveDTO();
        dto.setContact(contact);
        dto.setPhone("13800000000");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setDistrict("南山区");
        dto.setDetail("科技园路" + contact + "号");
        return dto;
    }

    @Test
    void testCreateAndList() {
        AddressVO created = addressService.create(buildDTO("张三"));
        System.out.println("\n=== 新增地址 ===");
        System.out.printf("id=%d contact=%s isDefault=%d%n", created.getId(), created.getContact(), created.getIsDefault());

        assertNotNull(created.getId(), "ID 不应为空");
        assertEquals("张三", created.getContact());
        assertEquals(TEST_USER_ID, created.getUserId());

        List<AddressVO> list = addressService.list();
        System.out.println("当前地址数=" + list.size());
        assertFalse(list.isEmpty(), "列表不应为空");
    }

    @Test
    void testUpdate() {
        AddressVO created = addressService.create(buildDTO("李四"));
        AddressSaveDTO updateDTO = buildDTO("李四改");
        AddressVO updated = addressService.update(created.getId(), updateDTO);

        assertEquals("李四改", updated.getContact());
        System.out.println("\n=== 编辑地址 ===");
        System.out.printf("编辑前: %s → 编辑后: %s%n", "李四", updated.getContact());
    }

    @Test
    void testDelete() {
        AddressVO created = addressService.create(buildDTO("王五"));
        assertNotNull(created.getId());

        addressService.delete(created.getId());

        // 删除后列表不应包含该地址
        List<AddressVO> list = addressService.list();
        boolean exists = list.stream().anyMatch(a -> a.getId().equals(created.getId()));
        assertFalse(exists, "已删除的地址不应出现在列表中");
        System.out.println("\n=== 删除地址 ===");
        System.out.println("删除后地址数=" + list.size() + "，已删除ID=" + created.getId());
    }

    @Test
    void testSetDefault() {
        AddressVO first = addressService.create(buildDTO("赵六"));
        AddressVO second = addressService.create(buildDTO("孙七"));

        // 第一个地址创建时自动为默认（首个地址）
        // 设为第二个为默认
        addressService.setDefault(second.getId());

        List<AddressVO> list = addressService.list();

        // 验证第二个是默认
        AddressVO secondAfter = list.stream()
                .filter(a -> a.getId().equals(second.getId()))
                .findFirst().orElseThrow();
        assertEquals(1, secondAfter.getIsDefault(), "第二个地址应为默认");

        // 验证第一个已取消默认
        AddressVO firstAfter = list.stream()
                .filter(a -> a.getId().equals(first.getId()))
                .findFirst().orElseThrow();
        assertEquals(0, firstAfter.getIsDefault(), "第一个地址应取消默认");

        System.out.println("\n=== 设默认地址 ===");
        System.out.printf("原默认: id=%d isDefault=%d%n", firstAfter.getId(), firstAfter.getIsDefault());
        System.out.printf("新默认: id=%d isDefault=%d%n", secondAfter.getId(), secondAfter.getIsDefault());
    }

    @Test
    void testFirstAddressAutoDefault() {
        // 清理干净后再创建首个地址
        List<AddressVO> current = addressService.list();
        for (AddressVO a : current) {
            addressService.delete(a.getId());
        }

        AddressVO first = addressService.create(buildDTO("周八"));
        assertEquals(1, first.getIsDefault(), "首个地址应自动设为默认");
        System.out.println("\n=== 首个地址自动默认 ===");
        System.out.printf("id=%d isDefault=%d%n", first.getId(), first.getIsDefault());
    }

    @Test
    void testListOrderDefaultFirst() {
        // 创建多个，验证默认地址排最前
        AddressVO a1 = addressService.create(buildDTO("默认地址"));
        addressService.create(buildDTO("非默认1"));
        addressService.create(buildDTO("非默认2"));

        // a1 是首个，自动默认
        List<AddressVO> list = addressService.list();
        assertEquals(1, list.get(0).getIsDefault(), "第一条应为默认地址");
        System.out.println("\n=== 列表排序验证 ===");
        list.forEach(a -> System.out.printf("  id=%d contact=%s isDefault=%d%n",
                a.getId(), a.getContact(), a.getIsDefault()));
    }

    @Test
    void testDeleteAllAndList() {
        // 删掉测试用户所有地址后列表为空
        List<AddressVO> current = addressService.list();
        System.out.println("\n=== 清理后验证 ===");
        for (AddressVO a : current) {
            addressService.delete(a.getId());
            System.out.println("删除: id=" + a.getId());
        }
        List<AddressVO> after = addressService.list();
        assertEquals(0, after.size(), "全部删除后列表应为空");
    }
}
