package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>团长审核请求</h2>
 *
 * <p>管理员审核团长申请时填写，通过或拒绝.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
public class GroupLeaderReviewDTO {

    /** 是否通过：true=通过 false=拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /** 拒绝原因（拒绝时必填，业务层校验） */
    private String rejectReason;
}
