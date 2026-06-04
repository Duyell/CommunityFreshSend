package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.LoginDTO;
import com.duyell.communityfreshdelivery.dto.RegisterDTO;
import com.duyell.communityfreshdelivery.dto.LoginVO;
import com.duyell.communityfreshdelivery.dto.RegisterVO;

/**
 * <h2>认证服务</h2>
 *
 * @author duyell
 * @since 2026-06-03
 */
public interface AuthService {

    /**
     * 手机号 + 密码登录，认证成功返回 JWT 令牌和用户信息.
     *
     * @param dto 手机号 + 明文密码
     * @return 登录响应（token + 用户信息）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 新用户注册，注册成功后自动登录并返回 JWT 令牌.
     *
     * @param dto 手机号 + 密码 + 昵称
     * @return 注册响应（token + 用户信息）
     */
    RegisterVO register(RegisterDTO dto);

    /**
     * 登出，将 token 加入 Redis 黑名单使其失效.
     *
     * <p>token 不存在或已过期时静默成功（幂等）.</p>
     *
     * @param token JWT 字符串（从 Authorization 头截取）
     */
    void logout(String token);
}
