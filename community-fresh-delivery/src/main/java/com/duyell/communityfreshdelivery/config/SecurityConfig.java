package com.duyell.communityfreshdelivery.config;

import com.duyell.communityfreshdelivery.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h2>Spring Security 安全配置</h2>
 *
 * <p>JWT 无状态认证方案的核心配置，串联 <b>JWT 过滤器 → DaoAuthenticationProvider → 权限校验</b> 整条链路.</p>
 *
 * <h3>认证架构</h3>
 * <pre>{@code
 * 请求 → JwtAuthenticationFilter（从 Authorization 头解析 JWT → 设置 SecurityContext）
 *      → 无需认证的路径放行
 *      → 其余请求走 SecurityContext 校验
 * }</pre>
 *
 * <h3>公开接口清单</h3>
 * <ul>
 *   <li>{@code POST /api/auth/login}      — 登录</li>
 *   <li>{@code POST /api/auth/register}   — 注册</li>
 *   <li>{@code /doc.html, /swagger-ui/**, /v3/api-docs/**} — Knife4j 文档</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 无需认证即可访问的路径 */
    private static final String[] PERMIT_ALL_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            // 分类/商品浏览 — URL 层全放行，鉴权由 @PreAuthorize 控制
            "/api/category/**",
            "/api/product/**",
            // 自提点列表 — 下单前即可查看
            "/api/pickup-point/**",
            // 商品评价 — 公开浏览
            "/api/review/product/**",
            // Knife4j
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    // ==================== 过滤器链 ====================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF — JWT 天然防跨站，无需 CSRF token
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 无状态会话 — 不创建 HttpSession，每次请求独立认证
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 路由权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                )

                // 4. JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // 5. 认证/授权异常 → 统一 JSON 响应
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    // ==================== 认证管理器 ====================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ==================== 密码编码器 ====================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== 异常处理器 ====================

    /**
     * 未认证（未登录/token 无效） → 返回 401.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(
                    "{\"code\":401,\"msg\":\"未登录或 token 已过期\",\"data\":null}"
            );
        };
    }

    /**
     * 权限不足（角色不匹配） → 返回 403.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(403);
            response.getWriter().write(
                    "{\"code\":403,\"msg\":\"权限不足\",\"data\":null}"
            );
        };
    }
}
