package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <h2>用户 Mapper</h2>
 *
 * <p>继承 {@link BaseMapper} 获得 MyBatis-Plus 内置 CRUD，
 * 复杂查询或需要语义化的方法在此声明.</p>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户（含逻辑删除过滤）.
     *
     * @param phone 手机号
     * @return User 实体，不存在时返回 {@code null}
     */
    @Select("SELECT * FROM user WHERE phone = #{phone} AND deleted = 0 LIMIT 1")
    User selectByPhone(String phone);
}
