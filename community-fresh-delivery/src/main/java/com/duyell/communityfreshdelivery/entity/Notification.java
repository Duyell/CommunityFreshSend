package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>站内消息实体</h2>
 *
 * <p>对应 {@code notification} 表。用于系统向用户推送通知（到货提醒、退款进度等）.
 * 用户登录/刷新页面时拉取未读消息.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("notification")
public class Notification {

    /** 消息ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型：1=系统通知 2=订单通知 3=退款通知 4=到货提醒 */
    private Integer type;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    /** 发送时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
