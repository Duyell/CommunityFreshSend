package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>支付流水 Mapper</h2>
 *
 * <p>幂等由数据库 {@code uk_pay_no} 唯一索引保证，应用层无需额外校验.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
