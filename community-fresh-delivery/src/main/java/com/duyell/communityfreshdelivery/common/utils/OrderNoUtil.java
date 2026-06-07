package com.duyell.communityfreshdelivery.common.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <h2>订单编号生成器</h2>
 *
 * <p>规则：{@code yyyyMMddHHmmss}（14位时间戳）+ 8位随机码（大写字母+数字），共22位.
 * 随机字符集排除易混淆字符（0/O/I/L/Z/2），确保人工可读.</p>
 *
 * <h3>示例</h3>
 * <pre>{@code 20260607153042A3KF9MZ2}</pre>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Component
public class OrderNoUtil {

    /** 排除 0/O/I/L/1/Z/2 的字符集，共 30 个字符 */
    private static final char[] CHARS = "ABCDEFGHJKMNPQRSTUVWXY3456789".toCharArray();

    private static final int RANDOM_LEN = 8;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成一个唯一的22位订单编号.
     *
     * @return 订单编号，如 {@code 20260607153042A3KF9MZ2}
     */
    public String generate() {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        StringBuilder sb = new StringBuilder(14 + RANDOM_LEN);
        sb.append(timestamp);
        for (int i = 0; i < RANDOM_LEN; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }
}
