package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <h2>分类树节点</h2>
 *
 * <p>递归结构：每个节点包含自身信息 + 子节点列表，前端可直接渲染为级联菜单或树形导航.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
@Builder
public class CategoryVO {

    /** 分类 ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 排序 */
    private Integer sort;

    /** 子分类列表 */
    private List<CategoryVO> children;
}
