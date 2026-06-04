package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>商品实体</h2>
 *
 * <p>对应 {@code product} 表。商品主信息存此表，规格/价格/库存由 {@link ProductSku} 管理.
 * 状态枚举：1=上架 2=下架 3=售罄.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
@TableName("product")
public class Product {

    /** 商品 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类 ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品图片 OSS Key 列表（JSON 数组字符串） */
    private String images;

    /** 状态：1=上架 2=下架 3=售罄 */
    private Integer status;

    /** 计价方式：0=固定规格 1=称重计价 */
    private Integer isWeighted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
