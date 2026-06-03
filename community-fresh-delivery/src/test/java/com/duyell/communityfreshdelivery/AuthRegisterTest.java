package com.duyell.communityfreshdelivery;

import com.duyell.communityfreshdelivery.dto.RegisterDTO;
import com.duyell.communityfreshdelivery.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 注册流程集成测试 —— 排查 500 错误.
 */
@SpringBootTest
class AuthRegisterTest {

    @Autowired
    private AuthService authService;

    @Test
    void testRegister() {
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13999999999");
        dto.setPassword("123456");
        dto.setNickname("集成测试用户");

        var result = authService.register(dto);
        System.out.println("\n=== 注册成功 ===");
        System.out.println("userId=" + result.getUserId());
        System.out.println("token=" + result.getToken());
    }

    @Test
    void testRegisterDuplicate() {
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone("13800000001"); // init.sql 已有
        dto.setPassword("123456");
        dto.setNickname("重复测试");

        authService.register(dto); // 应抛出 BusinessException
    }
}
