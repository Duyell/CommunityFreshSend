package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>配送员审核请求</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class DeliveryReviewDTO {

    /** 是否通过 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /** 拒绝原因（拒绝时必填） */
    private String rejectReason;
}
