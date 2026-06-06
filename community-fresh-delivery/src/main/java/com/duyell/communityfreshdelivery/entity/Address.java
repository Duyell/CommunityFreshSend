package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>收货地址实体</h2>
 *
 * <p>对应 {@code address} 表，一个用户最多 10 个地址，有且仅有一个默认地址（is_default=1）.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Data
@TableName("address")
public class Address {

    /** 地址 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 收货人姓名 */
    private String contact;

    /** 收货人电话 */
    private String phone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址（门牌号） */
    private String detail;

    /** 是否默认地址：1=默认 */
    private Integer isDefault;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
