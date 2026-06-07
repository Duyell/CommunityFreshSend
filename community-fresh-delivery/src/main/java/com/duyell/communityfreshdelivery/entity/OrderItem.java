package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>订单明细实体</h2>
 *
 * <p>对应 {@code order_item} 表。下单时快照商品名/规格/单价，
 * 称重商品分拣后更新实重并调整金额.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@TableName("order_item")
public class OrderItem {

    /** 明细ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 规格ID */
    private Long skuId;

    /** 商品名称快照（下单时的名称） */
    private String productName;

    /** 规格名称快照（下单时的规格） */
    private String specName;

    /** 下单时单价 */
    private BigDecimal price;

    /** 下单数量 */
    private Integer quantity;

    /** 预估重量（斤，称重商品用，Phase 2） */
    private BigDecimal estimatedWeight;

    /** 分拣实重（斤，称重商品用，Phase 2） */
    private BigDecimal actualWeight;

    /** 小计金额（price × quantity，预估） */
    private BigDecimal amount;

    /** 分拣后调整金额（实重×单价，Phase 2） */
    private BigDecimal actualAmount;

    /** 缺货标记：1=缺货 */
    private Integer shortage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
