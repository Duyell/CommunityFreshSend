package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>配送记录实体</h2>
 *
 * <p>对应 {@code delivery} 表。一个订单最多一条配送记录，
 * 记录配送员抢单/取货/送达的时间节点.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@TableName("delivery")
public class Delivery {

    /** 配送记录ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 配送员用户ID */
    private Long deliveryUserId;

    /** 配送状态：1=待取货 2=配送中 3=已送达 */
    private Integer status;

    /** 取货确认时间（去仓库取到货时记录） */
    private LocalDateTime pickupTime;

    /** 送达确认时间（交付给用户或自提点时记录） */
    private LocalDateTime deliverTime;

    /** 创建时间（抢单时间） */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
