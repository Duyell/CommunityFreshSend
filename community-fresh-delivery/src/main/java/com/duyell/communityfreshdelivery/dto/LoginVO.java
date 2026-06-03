package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <h2>登录响应</h2>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Data
@Builder
public class LoginVO {

    /** 用户 ID */
    private Long userId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像 OSS Key */
    private String avatar;

    /** JWT 令牌，后续请求带在 Authorization: Bearer <token> */
    private String token;

    /** 角色列表（如 ROLE_USER, ROLE_DELIVERY） */
    private List<String> roles;
}
