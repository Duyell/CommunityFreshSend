package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <h2>用户角色关联 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 物理删除用户角色（绕过 @TableLogic）。
     * 用于 re-grant 场景：软删除后唯一约束仍占位，需先物理清除再插入.
     *
     * @param userId 用户ID
     * @param role   角色字符串
     * @return 删除行数
     */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId} AND role = #{role}")
    int physicalDelete(@Param("userId") Long userId, @Param("role") String role);
}
