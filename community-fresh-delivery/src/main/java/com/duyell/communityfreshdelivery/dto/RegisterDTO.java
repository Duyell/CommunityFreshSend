package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <h2>注册请求</h2>
 *
 * @author duyell
 * @since 2026-06-03
 */
@Data
public class RegisterDTO {

    /** 手机号（11 位数字） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 明文密码（6-20 位） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;
}
