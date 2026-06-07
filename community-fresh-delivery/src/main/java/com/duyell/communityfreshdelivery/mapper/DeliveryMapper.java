package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Delivery;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>配送记录 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Mapper
public interface DeliveryMapper extends BaseMapper<Delivery> {
}
