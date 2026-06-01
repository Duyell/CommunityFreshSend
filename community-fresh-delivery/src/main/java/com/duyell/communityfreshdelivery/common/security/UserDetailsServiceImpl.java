package com.duyell.communityfreshdelivery.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.entity.User;
import com.duyell.communityfreshdelivery.entity.UserRole;
import com.duyell.communityfreshdelivery.mapper.UserMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>Spring Security 用户加载服务</h2>
 *
 * <p>实现 {@link UserDetailsService#loadUserByUsername}，以手机号作为登录标识，
 * 查询 {@code user} 表获取基本信息，查 {@code user_role} 表获取角色列表，
 * 组装为 {@link UserDetailsImpl} 返回给 Spring Security 认证链.</p>
 *
 * <h3>认证链路</h3>
 * <pre>{@code
 * AuthController → AuthenticationManager
 *     → DaoAuthenticationProvider
 *         → UserDetailsServiceImpl.loadUserByUsername(phone)  ← 本类
 *             → UserMapper.selectOne(phone)   → 获取 userId + 加密密码
 *             → UserRoleMapper.selectList(userId) → 获取角色列表
 *             → new UserDetailsImpl(userId, password, roles)
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    /**
     * 根据手机号加载用户.
     *
     * @param phone 手机号（作为登录用户名）
     * @return UserDetails 包含 userId + 加密密码 + roles
     * @throws UsernameNotFoundException 手机号未注册时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 1. 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + phone);
        }

        // 2. 查角色
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        List<String> roles = userRoles.stream()
                .map(UserRole::getRole)
                .toList();

        log.debug("加载用户: userId={}, roles={}", user.getId(), roles);

        // 3. 组装 UserDetails，密码由 DaoAuthenticationProvider 自动比对
        return new UserDetailsImpl(user.getId(), user.getPassword(), roles);
    }
}
