package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>商品 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
