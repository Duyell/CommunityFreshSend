package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>订单 Mapper</h2>
 *
 * <p>增删改由 MyBatis-Plus BaseMapper 提供，复杂查询通过 LambdaQueryWrapper 动态拼装.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
