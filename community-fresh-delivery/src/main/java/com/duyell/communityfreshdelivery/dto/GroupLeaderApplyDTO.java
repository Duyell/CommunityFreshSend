package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * <h2>团长申请请求</h2>
 *
 * <p>普通用户提交团长申请时填写，包含自提点地址和联系人信息.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
public class GroupLeaderApplyDTO {

    /** 自提点地址（小区名+门牌号） */
    @NotBlank(message = "自提点地址不能为空")
    private String address;

    /** 联系人姓名 */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /** 附言（可选） */
    private String remark;
}
