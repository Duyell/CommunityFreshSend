package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>操作日志实体</h2>
 *
 * <p>对应 {@code operation_log} 表。记录订单状态变更、审核等关键操作.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("operation_log")
public class OperationLog {

    /** 日志ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人用户ID（系统操作时为NULL） */
    private Long userId;

    /** 操作动作（如 ORDER_CANCEL、REVIEW_APPROVE） */
    private String action;

    /** 操作对象类型（如 ORDER、USER） */
    private String targetType;

    /** 操作对象ID */
    private Long targetId;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作详情 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
