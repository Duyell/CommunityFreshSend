package com.duyell.communityfreshdelivery.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * <h2>JWT 令牌工具类</h2>
 *
 * <p>负责令牌的生成、解析与校验，作为 Spring Bean 注入到 Security 过滤器和登录服务中.
 * 密钥与过期时间从 {@code application.yml} 的 {@code app.jwt.*} 读取.</p>
 *
 * <h3>Token 载荷结构</h3>
 * <pre>{@code
 * {
 *   "sub": "userId",         // 用户 ID（字符串形式）
 *   "roles": ["ROLE_USER"],  // 角色列表
 *   "iat": 1717200000,       // 签发时间
 *   "exp": 1717286400        // 过期时间
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 登录成功 → 签发 token
 * String token = jwtUtil.generateToken(1L, List.of("ROLE_USER"));
 *
 * // 请求到达 → 校验并提取用户信息
 * Long userId = jwtUtil.getUserIdFromToken(token);
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * @param secret     JWT 签名密钥（至少 256 位），从 {@code app.jwt.secret} 注入
     * @param expiration Token 有效期（毫秒），从 {@code app.jwt.expiration} 注入，默认 24 小时
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 签发 JWT 令牌.
     *
     * @param userId 用户 ID
     * @param roles  角色列表（如 {@code ROLE_USER, ROLE_DELIVERY}）
     * @return 签名字符串，前端存 localStorage，后续请求带在 {@code Authorization: Bearer <token>} 头中
     */
    public String generateToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 token 中提取用户 ID.
     * <p>调用方需确保 token 已通过 {@link #isTokenValid} 校验，否则可能抛出异常.</p>
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        String sub = parseClaims(token).getSubject();
        return Long.valueOf(sub);
    }

    /**
     * 从 token 中提取角色列表.
     *
     * @param token JWT 字符串，调用方需确保已通过 {@link #isTokenValid} 校验
     * @return 角色列表（如 {@code ["ROLE_USER", "ROLE_DELIVERY"]}），claim 不存在时返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object rolesObj = parseClaims(token).get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    /**
     * 校验 token 是否合法且在有效期内.
     *
     * @param token JWT 字符串
     * @return {@code true} 合法且未过期
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 已过期: {}", e.getMessage());
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.debug("JWT 非法: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 解析 token 并返回 Claims，内部统一处理各类 JWT 异常.
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     * @throws ExpiredJwtException      token 已过期
     * @throws SecurityException        token 签名不匹配
     * @throws MalformedJwtException    token 格式错误
     * @throws UnsupportedJwtException  token 类型不支持
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
