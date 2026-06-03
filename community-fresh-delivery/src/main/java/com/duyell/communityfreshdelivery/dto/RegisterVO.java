package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <h2>注册响应</h2>
 *
 * <p>注册成功即登录，直接返回 JWT 令牌.</p>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Data
@Builder
public class RegisterVO {

    /** 用户 ID */
    private Long userId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** JWT 令牌（注册即登录） */
    private String token;

    /** 角色列表（新注册用户固定为 ROLE_USER） */
    private List<String> roles;
}
