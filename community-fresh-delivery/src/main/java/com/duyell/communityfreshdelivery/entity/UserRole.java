package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>用户角色关联实体</h2>
 *
 * <p>对应 {@code user_role} 表。一个用户可持有多个角色（如居民 + 团长），
 * 角色以字符串形式存储（{@code ROLE_USER / ROLE_DELIVERY / ROLE_MERCHANT / ROLE_GROUP_LEADER / ROLE_ADMIN}）.</p>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Data
@TableName("user_role")
public class UserRole {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 角色字符串 */
    private String role;

    /** 授权时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
