package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>团长申请实体</h2>
 *
 * <p>对应 {@code group_leader_application} 表。普通用户提交申请后由管理员审核，
 * 通过后自动创建 {@code pickup_point} 并追加 {@code ROLE_GROUP_LEADER} 角色.</p>
 *
 * <h3>审核状态</h3>
 * <pre>{@code
 * 0 = 待审核
 * 1 = 已通过
 * 2 = 已拒绝
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
@TableName("group_leader_application")
public class GroupLeaderApplication {

    /** 申请ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 自提点地址（小区名+门牌号） */
    private String address;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 附言（可选） */
    private String remark;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    /** 拒绝原因（审核拒绝时填写） */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 申请时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
