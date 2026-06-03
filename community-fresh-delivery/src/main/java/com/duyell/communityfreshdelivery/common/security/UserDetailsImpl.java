package com.duyell.communityfreshdelivery.common.security;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * <h2>Spring Security UserDetails 实现</h2>
 *
 * <p>连接 <b>项目自有用户模型</b> 与 <b>Spring Security 认证体系</b> 的适配器.
 * 不依赖数据库实体，由 {@link UserDetailsServiceImpl} 或 {@code JwtAuthenticationFilter} 构造.</p>
 *
 * <h3>构造来源</h3>
 * <ul>
 *   <li><b>登录时</b> — {@link UserDetailsServiceImpl} 查库后构建</li>
 *   <li><b>请求鉴权时</b> — {@code JwtAuthenticationFilter} 从 JWT 载荷中提取 userId + roles 后构建</li>
 * </ul>
 *
 * <h3>角色格式</h3>
 * <p>角色以 {@code ROLE_} 前缀存储，存入 {@link SimpleGrantedAuthority} 时自动补全前缀.</p>
 *
 * @author duyell
 * @since 2026-06-01
 */
public class UserDetailsImpl implements UserDetails {

    /** 用户 ID */
    @Getter
    private final long userId;

    /** 加密后的密码（BCrypt）。JWT 认证时为 null（token 已验证，无需密码） */
    private final String password;

    /** 角色列表（如 {@code ROLE_USER, ROLE_DELIVERY}） */
    @Getter
    @NonNull
    private final List<String> roles;

    /**
     * @param userId   用户 ID
     * @param password 加密后的密码（BCrypt 密文）
     * @param roles    角色列表，含 {@code ROLE_} 前缀
     */
    public UserDetailsImpl(Long userId, String password, List<String> roles) {
        this.userId = userId;
        this.password = password;
        this.roles = roles;
    }

    /**
     * 将角色列表转换为 Spring Security 的 GrantedAuthority 集合.
     * <p>自动补齐 {@code ROLE_} 前缀，避免调用方遗漏.</p>
     */
    @NonNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    // ---- 用户名取 userId 的字符串形式 ----

    @NonNull
    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    // ---- 账户状态：始终有效（后续可扩展为从用户表读取状态字段） ----

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
