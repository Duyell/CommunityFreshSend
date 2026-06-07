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

    /**
     * 扣减库存（WHERE stock >= quantity 防超卖，返回受影响行数判成败）.
     *
     * @param skuId    规格ID
     * @param quantity 扣减数量
     * @return 1=成功 0=库存不足
     */
    @Update("UPDATE product_sku SET stock = stock - #{quantity} WHERE id = #{skuId} AND stock >= #{quantity}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /**
     * 回补库存（取消订单/超时取消时用）.
     *
     * @param skuId    规格ID
     * @param quantity 回补数量
     */
    @Update("UPDATE product_sku SET stock = stock + #{quantity} WHERE id = #{skuId}")
    int addStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
