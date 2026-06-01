package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper.
 * @author duyell
 * @since 2026-06-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
