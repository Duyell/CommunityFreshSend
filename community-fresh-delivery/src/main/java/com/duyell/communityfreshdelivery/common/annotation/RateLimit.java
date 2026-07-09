package com.duyell.communityfreshdelivery.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>接口限流注解</h2>
 *
 * <p>基于 Redis ZSet 滑动窗口实现。标记在 Controller 方法上，
 * 由 {@code RateLimitAspect} 切面拦截校验.</p>
 *
 * <h3>示例</h3>
 * <pre>{@code
 * @RateLimit(key = "order:place", limit = 5, window = 60) // 60秒内最多5次
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流 key（业务标识，如 "order:place"） */
    String key();

    /** 时间窗口内允许的最大请求次数 */
    int limit() default 10;

    /** 时间窗口大小（秒） */
    int window() default 60;

    /** 超限提示消息 */
    String message() default "请求过于频繁，请稍后再试";
}
