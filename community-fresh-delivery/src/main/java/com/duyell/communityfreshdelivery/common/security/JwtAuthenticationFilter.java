package com.duyell.communityfreshdelivery.common.security;

import com.duyell.communityfreshdelivery.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
 * 请求 → extractToken() → isTokenValid()? → 检查 Redis 黑名单（已登出?）
 *   ├── 任一步失败 → 跳过，继续 filter 链（SecurityConfig 返回 401）
 *   └── 全部通过 → getUserIdFromToken() + getRolesFromToken()
 *                    → new UserDetailsImpl(userId, null, roles, true)
 *                    → UsernamePasswordAuthenticationToken → SecurityContextHolder
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
    private final StringRedisTemplate redisTemplate;

    /** Redis 中 JWT 黑名单的 key 前缀，与 {@code AuthServiceImpl} 保持一致 */
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = JwtUtil.extractBearerToken(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            // 检查 token 是否在黑名单中（已登出）
            if (redisTemplate.hasKey(JWT_BLACKLIST_PREFIX + token)) {
                log.debug("JWT 认证失败：token 已登出（命中黑名单）");
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            List<String> roles = jwtUtil.getRolesFromToken(token);

            // 从 JWT 构建 UserDetails（无需密码，JWT 签名已验证；enabled=true 因为登录时已检查状态）
            UserDetailsImpl userDetails = new UserDetailsImpl(userId, null, roles, true);

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

}
