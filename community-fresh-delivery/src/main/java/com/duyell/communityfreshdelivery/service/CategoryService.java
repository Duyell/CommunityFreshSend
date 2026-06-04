package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.CategoryVO;

import java.util.List;

/**
 * <h2>商品分类服务</h2>
 *
 * @author duyell
 * @since 2026-06-04
 */
public interface CategoryService {

    /**
     * 获取启用的分类树.
     *
     * <p>一次性查询全表，内存中按 {@code parentId} 组装成树，返回一级分类列表.</p>
     *
     * @return 一级分类集合，每个节点递归包含子分类
     */
    List<CategoryVO> getCategoryTree();
}
