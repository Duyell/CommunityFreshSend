package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <h2>提货码核销请求</h2>
 *
 * <p>团长输入用户出示的 6 位数字提货码完成核销.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
public class PickupCodeDTO {

    /** 6位数字提货码 */
    @NotBlank(message = "提货码不能为空")
    private String pickupCode;
}
