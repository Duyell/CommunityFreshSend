package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>收货地址 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
