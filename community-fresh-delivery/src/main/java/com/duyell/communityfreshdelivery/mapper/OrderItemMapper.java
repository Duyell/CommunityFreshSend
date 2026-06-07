package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>订单明细 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
