package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.dto.CategoryVO;
import com.duyell.communityfreshdelivery.entity.Category;
import com.duyell.communityfreshdelivery.mapper.CategoryMapper;
import com.duyell.communityfreshdelivery.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/**
 * <h2>商品分类服务实现</h2>
 *
 * <p>分类树一次性全表查询后在内存中组装，避免 N+1 递归 SQL.
 * 社区项目分类总量有限（几十条），全量加载无性能问题.</p>
 *
 * <h3>组装逻辑</h3>
 * <ol>
 *   <li>{@code SELECT * FROM category WHERE status=1 AND deleted=0 ORDER BY sort}</li>
 *   <li>按 {@code parentId} 分组为 Map</li>
 *   <li>从 {@code parentId=0}（一级分类）开始递归构建子树</li>
 * </ol>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 1. 查全表已启用的分类，按 sort 排序
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        if (all.isEmpty()) {
            return List.of();
        }

        // 2. 按 parentId 分组
        Map<Long, List<Category>> grouped = all.stream()
                .collect(groupingBy(Category::getParentId));

        // 3. 从根节点（parentId=0）开始递归构建
        return buildChildren(0L, grouped);
    }

    /**
     * 递归构建子节点列表.
     *
     * @param parentId 父分类 ID
     * @param grouped  按 parentId 分组的所有分类
     * @return 该父节点下的 CategoryVO 列表
     */
    private List<CategoryVO> buildChildren(Long parentId, Map<Long, List<Category>> grouped) {
        List<Category> children = grouped.getOrDefault(parentId, List.of());
        List<CategoryVO> result = new ArrayList<>(children.size());
        for (Category cat : children) {
            result.add(CategoryVO.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .sort(cat.getSort())
                    .children(buildChildren(cat.getId(), grouped))
                    .build());
        }
        return result;
    }
}
