package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <h2>登录请求</h2>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Data
public class LoginDTO {

    /** 手机号（登录用户名） */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 明文密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
