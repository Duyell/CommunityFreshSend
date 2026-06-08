package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>团长申请响应</h2>
 *
 * <p>返回申请状态给用户或管理员查看，含审核结果和拒绝原因.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
@Builder
public class GroupLeaderApplicationVO {

    /** 申请ID */
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 自提点地址 */
    private String address;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 附言 */
    private String remark;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    /** 审核状态文本 */
    private String statusText;

    /** 拒绝原因（status=2时有值） */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 申请时间 */
    private LocalDateTime createTime;
}
