package com.duyell.communityfreshdelivery.common.utils;

import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <h2>安全上下文工具</h2>
 *
 * <p>从 Spring Security 的 SecurityContext 中提取当前登录用户信息.
 * 所有方法均为静态方法，无需注入，可直接调用.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 获取当前登录用户的 ID.
     *
     * @return 当前用户 ID
     */
    public static Long currentUserId() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUserId();
    }
}
