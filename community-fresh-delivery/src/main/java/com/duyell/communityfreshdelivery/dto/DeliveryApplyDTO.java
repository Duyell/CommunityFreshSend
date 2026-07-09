package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <h2>配送员申请请求</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class DeliveryApplyDTO {

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    /** 附言（可选） */
    private String remark;
}
