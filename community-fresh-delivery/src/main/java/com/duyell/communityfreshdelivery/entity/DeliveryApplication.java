package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>配送员申请实体</h2>
 *
 * <p>对应 {@code delivery_application} 表。普通用户提交配送员申请，
 * 管理员审核通过后追加 ROLE_DELIVERY 角色.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("delivery_application")
public class DeliveryApplication {

    /** 申请ID，自增 */
    @TableId(type = IdType.AUTO)
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

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 申请时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
