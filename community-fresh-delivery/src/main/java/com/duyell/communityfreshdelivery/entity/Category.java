package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>商品分类实体</h2>
 *
 * <p>对应 {@code category} 表，树形结构（parent_id=0 为一级分类）。
 * 分类树在 Service 层一次性全表查询后内存组装，不做递归 SQL.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
@TableName("category")
public class Category {

    /** 分类 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，0=一级分类 */
    private Long parentId;

    /** 分类名称（如"热带水果"） */
    private String name;

    /** 排序（越小越前） */
    private Integer sort;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
