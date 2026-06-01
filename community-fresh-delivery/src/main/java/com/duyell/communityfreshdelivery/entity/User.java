package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>用户实体</h2>
 *
 * <p>对应 {@code user} 表。登录以手机号作为唯一标识，密码使用 BCrypt 加密存储.</p>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Data
@TableName("user")
public class User {

    /** 用户 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（登录用户名），唯一索引 */
    private String phone;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 OSS Key */
    private String avatar;

    /** 账号状态：1=正常 0=禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：1=已删除 */
    @TableLogic
    private Integer deleted;
}
