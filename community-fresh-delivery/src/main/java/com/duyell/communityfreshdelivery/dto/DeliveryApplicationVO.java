package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>配送员申请响应</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class DeliveryApplicationVO {

    /** 申请ID */
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 联系电话 */
    private String phone;

    /** 附言 */
    private String remark;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 申请时间 */
    private LocalDateTime createTime;
}
