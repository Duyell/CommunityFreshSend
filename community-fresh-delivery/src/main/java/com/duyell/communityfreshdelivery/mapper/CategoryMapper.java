package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>商品分类 Mapper</h2>
 *
 * <p>继承 {@link BaseMapper} 获得 MyBatis-Plus 内置 CRUD.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
