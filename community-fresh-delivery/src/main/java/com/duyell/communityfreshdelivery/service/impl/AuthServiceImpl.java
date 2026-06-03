package com.duyell.communityfreshdelivery.service.impl;

import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.security.UserDetailsImpl;
import com.duyell.communityfreshdelivery.common.utils.JwtUtil;
import com.duyell.communityfreshdelivery.dto.LoginDTO;
import com.duyell.communityfreshdelivery.dto.RegisterDTO;
import com.duyell.communityfreshdelivery.entity.User;
import com.duyell.communityfreshdelivery.entity.UserRole;
import com.duyell.communityfreshdelivery.mapper.UserMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import com.duyell.communityfreshdelivery.service.AuthService;
import com.duyell.communityfreshdelivery.dto.LoginVO;
import com.duyell.communityfreshdelivery.dto.RegisterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>认证服务实现</h2>
 *
 * <p>登录链路：Controller → AuthService.login() → AuthenticationManager.authenticate()
 * → UserDetailsServiceImpl.loadUserByUsername() → 校验密码 → 签发 JWT.</p>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 委托 Spring Security 做认证（密码比对由 DaoAuthenticationProvider + BCrypt 完成）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword())
        );

        // 2. 认证成功 → 提取用户信息
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        List<String> roles = userDetails.getRoles();

        // 3. 查用户表获取昵称、头像等展示信息
        User user = userMapper.selectById(userId);

        // 4. 签发 JWT
        String token = jwtUtil.generateToken(userId, roles);

        log.info("用户登录成功: userId={}, roles={}", userId, roles);

        // 5. 组装响应
        return LoginVO.builder()
                .userId(userId)
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public RegisterVO register(RegisterDTO dto) {
        // 1. 校验手机号唯一性
        if (userMapper.selectByPhone(dto.getPhone()) != null) {
            throw new BusinessException(30001, "该手机号已注册");
        }

        // 2. 创建用户
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(1);
        userMapper.insert(user);

        // 3. 分配普通用户角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole("ROLE_USER");
        userRoleMapper.insert(userRole);

        // 4. 注册即登录 → 签发 JWT
        List<String> roles = List.of("ROLE_USER");
        String token = jwtUtil.generateToken(user.getId(), roles);

        log.info("用户注册成功: userId={}, phone={}", user.getId(), dto.getPhone());

        // 5. 组装响应
        return RegisterVO.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .token(token)
                .roles(roles)
                .build();
    }
}
