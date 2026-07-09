package com.duyell.communityfreshdelivery.common.aop;

import com.duyell.communityfreshdelivery.common.annotation.RateLimit;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * <h2>接口限流 AOP 切面</h2>
 *
 * <p>基于 Redis ZSet 滑动窗口算法：
 * <ol>
 *   <li>移除窗口外的过期记录: {@code ZREMRANGEBYSCORE 0 (now - window)}</li>
 *   <li>添加本次请求: {@code ZADD now}</li>
 *   <li>设 key 过期时间: {@code EXPIRE window + 1}</li>
 *   <li>统计窗口内请求数: {@code ZCARD} > limit → 拒绝</li>
 * </ol></p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "rate_limit:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 优先用 userId，未登录时用 IP
        String identity = resolveIdentity();
        String key = PREFIX + identity + ":" + rateLimit.key();
        int limit = rateLimit.limit();
        int window = rateLimit.window();

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (long) window * 1000;

        // 滑动窗口：移除过期记录 + 添加当前请求 + 设置过期
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        stringRedisTemplate.expire(key, Duration.ofSeconds(window + 1));

        // 统计窗口内请求数
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        if (count != null && count > limit) {
            log.warn("接口限流触发: key={} count={} limit={}", rateLimit.key(), count, limit);
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /** 获取用户标识：已登录用 userId，未登录用 IP */
    private String resolveIdentity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.duyell.communityfreshdelivery.common.security.UserDetailsImpl userDetails) {
            return String.valueOf(userDetails.getUserId());
        }
        // 未登录 → 用 IP
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
