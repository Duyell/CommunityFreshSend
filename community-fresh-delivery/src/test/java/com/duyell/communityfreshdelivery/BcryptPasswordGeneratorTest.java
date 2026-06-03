package com.duyell.communityfreshdelivery;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码生成工具 —— 为 init.sql 生成真实 BCrypt 密文.
 *
 * <p>使用方法：运行本测试，将控制台输出的 hash 粘贴到 init.sql 的 INSERT 语句中.</p>
 */
class BcryptPasswordGeneratorTest {

    private static final String RAW_PASSWORD = "123456";

    @Test
    void generatePasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("\n========== BCrypt 密码生成 (明文: " + RAW_PASSWORD + ") ==========\n");

        String[] nicknames = {"测试居民", "测试配送员", "测试商家", "测试管理员"};
        String[] phones = {"13800000001", "13800000002", "13800000003", "13800000004"};

        for (int i = 0; i < 4; i++) {
            String hash = encoder.encode(RAW_PASSWORD);
            System.out.printf("-- %s (%s)%n", nicknames[i], phones[i]);
            System.out.printf("'%s'%n", hash);
            System.out.println();
        }

        System.out.println("========================================\n");
        System.out.println("验证: " + encoder.matches(RAW_PASSWORD, encoder.encode(RAW_PASSWORD)));
    }
}
