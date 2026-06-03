package com.duyell.communityfreshdelivery.common.security;

import com.duyell.communityfreshdelivery.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * <h2>JWT 认证过滤器</h2>
 *
 * <p>每个请求到达时，从 {@code Authorization: Bearer <token>} 头中提取 JWT，
 * 校验合法性后从中解析出 userId + roles，构建 {@link UserDetailsImpl}
 * 并写入 {@link SecurityContextHolder}，后续 Controller 即可通过
 * {@code @AuthenticationPrincipal} 获取当前用户.</p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>无状态</b> — 不查数据库，完全信任 JWT 载荷（角色信息已在签发时写入 token）</li>
 *   <li><b>静默失败</b> — token 缺失/不合法时不抛异常，仅不设 SecurityContext，
 *       由 {@code SecurityConfig} 统一决定 401 响应</li>
 *   <li><b>每次请求都执行</b> — 继承 {@link OncePerRequestFilter}，保证一个请求只走一次</li>
 * </ul>
 *
 * <h3>认证流程</h3>
 * <pre>{@code
 * 请求 → extractToken() → isTokenValid()?
 *   ├── 否 → 跳过，继续 filter 链（SecurityConfig 会拦截未认证请求）
 *   └── 是 → getUserIdFromToken() + getRolesFromToken()
 *              → new UserDetailsImpl(userId, null, roles)
 *              → UsernamePasswordAuthenticationToken → SecurityContextHolder
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** Authorization 头的 token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LEN = BEARER_PREFIX.length();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            List<String> roles = jwtUtil.getRolesFromToken(token);

            // 从 JWT 构建 UserDetails（无需密码，JWT 签名已验证）
            UserDetailsImpl userDetails = new UserDetailsImpl(userId, null, roles);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 认证成功: userId={}, roles={}", userId, roles);
        }

        // token 缺失或不合法 → 不设 SecurityContext，继续执行
        // SecurityConfig 中配置的 .anyRequest().authenticated() 会返回 401
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer token.
     *
     * @param request HTTP 请求
     * @return token 字符串，不存在或格式不对时返回 {@code null}
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX_LEN);
        }
        return null;
    }
}
