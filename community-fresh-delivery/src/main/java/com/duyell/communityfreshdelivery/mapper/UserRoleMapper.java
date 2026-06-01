package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper.
 * @author duyell
 * @since 2026-06-01
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
