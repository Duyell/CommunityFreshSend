package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>优惠券模板实体</h2>
 *
 * <p>对应 {@code coupon} 表。券类型说明：
 * <ul>
 *   <li>1=满减券：threshold=最低消费，discountValue=立减金额</li>
 *   <li>2=折扣券：threshold=最低消费，discountValue=折扣率（0.8=8折）</li>
 *   <li>3=新人券：threshold=0，discountValue=立减金额</li>
 *   <li>4=品类券：threshold=最低消费，discountValue=立减金额，scopeType=1+scopeId=分类ID</li>
 * </ul></p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("coupon")
public class Coupon {

    /** 券模板ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称（如"满50减5"） */
    private String name;

    /** 券类型：1=满减券 2=折扣券 3=新人券 4=品类券 */
    private Integer type;

    /** 使用门槛金额（满减券/折扣券的最低消费金额） */
    private BigDecimal threshold;

    /** 优惠值（满减=减多少钱，折扣=0.8表示8折） */
    private BigDecimal discountValue;

    /** 适用范围：0=全场 1=指定分类 */
    private Integer scopeType;

    /** 适用分类ID（scopeType=1时有值） */
    private Long scopeId;

    /** 有效期天数（领取后N天内有效） */
    private Integer validDays;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
