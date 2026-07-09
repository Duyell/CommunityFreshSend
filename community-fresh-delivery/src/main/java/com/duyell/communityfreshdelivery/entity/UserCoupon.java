package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>用户持有优惠券实体</h2>
 *
 * <p>对应 {@code user_coupon} 表。记录用户领取/管理员发放的优惠券及使用状态.
 * status：0=未使用 1=已使用 2=已过期.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    /** 持有记录ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 券模板ID */
    private Long couponId;

    /** 状态：0=未使用 1=已使用 2=已过期 */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 领取时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
