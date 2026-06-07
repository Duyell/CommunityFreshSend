package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>自提点实体</h2>
 *
 * <p>对应 {@code pickup_point} 表。归属类型区分平台自营(1)和团长(2).</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@TableName("pickup_point")
public class PickupPoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 自提点名称 */
    private String name;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 自提点地址 */
    private String address;

    /** 归属类型：1=平台自营 2=团长 */
    private Integer ownerType;

    /** 团长用户ID（ownerType=2时有值） */
    private Long ownerId;

    /** 状态：1=营业 0=停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
