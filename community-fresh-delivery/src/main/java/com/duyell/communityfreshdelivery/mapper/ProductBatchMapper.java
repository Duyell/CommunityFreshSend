package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.ProductBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <h2>批次库存 Mapper</h2>
 *
 * <p>提供原子化的库存扣减方法，防超卖.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Mapper
public interface ProductBatchMapper extends BaseMapper<ProductBatch> {

    /**
     * 原子扣减批次剩余库存（WHERE remaining >= quantity 防超卖）.
     *
     * @param id       批次ID
     * @param quantity 扣减数量
     * @return affected rows（1=成功，0=库存不足或被并发修改）
     */
    @Update("UPDATE product_batch SET remaining = remaining - #{quantity} " +
            "WHERE id = #{id} AND remaining >= #{quantity}")
    int deductRemaining(@Param("id") Long id, @Param("quantity") int quantity);

    /** 原子回补库存（取消订单/缺货退款时使用） */
    @Update("UPDATE product_batch SET remaining = remaining + #{quantity} WHERE id = #{id}")
    int addRemaining(@Param("id") Long id, @Param("quantity") int quantity);
}
