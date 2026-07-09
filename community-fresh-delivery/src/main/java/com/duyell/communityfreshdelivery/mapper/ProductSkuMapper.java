package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.ProductSku;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <h2>商品规格 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 按商品 ID 删除所有规格（编辑商品时"先删后插"策略）.
     *
     * @param productId 商品 ID
     */
    @Delete("DELETE FROM product_sku WHERE product_id = #{productId}")
    void deleteByProductId(@Param("productId") Long productId);
}
