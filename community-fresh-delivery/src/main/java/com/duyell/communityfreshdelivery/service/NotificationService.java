package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.NotificationVO;

import java.util.List;

/**
 * <h2>站内消息服务</h2>
 *
 * <p>提供未读消息拉取、已读标记、消息发送、分页查询等功能.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface NotificationService {

    /**
     * 获取当前用户未读消息数量.
     *
     * @return 未读消息数
     */
    long getUnreadCount();

    /**
     * 获取当前用户未读消息列表.
     *
     * @return 未读消息列表（按时间倒序）
     */
    List<NotificationVO> getUnread();

    /**
     * 分页查询当前用户所有消息.
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<NotificationVO> page(int page, int size);

    /**
     * 标记单条消息为已读.
     *
     * @param id 消息ID
     */
    void markAsRead(Long id);

    /**
     * 全部标记为已读.
     */
    void markAllRead();

    /**
     * 发送站内消息（供其他 Service 调用）.
     *
     * @param userId  接收用户ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型：1=系统通知 2=订单通知 3=退款通知 4=到货提醒
     */
    void send(Long userId, String title, String content, Integer type);
}
