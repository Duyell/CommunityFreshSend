package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.NotificationVO;
import com.duyell.communityfreshdelivery.entity.Notification;
import com.duyell.communityfreshdelivery.mapper.NotificationMapper;
import com.duyell.communityfreshdelivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>站内消息服务实现</h2>
 *
 * <p>消息类型文本映射：1→系统通知 2→订单通知 3→退款通知 4→到货提醒.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public long getUnreadCount() {
        Long userId = SecurityUtil.currentUserId();
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }

    @Override
    public List<NotificationVO> getUnread() {
        Long userId = SecurityUtil.currentUserId();
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreateTime)
        );
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public Page<NotificationVO> page(int page, int size) {
        Long userId = SecurityUtil.currentUserId();

        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime)
        );

        List<NotificationVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        Page<NotificationVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public void markAsRead(Long id) {
        Long userId = SecurityUtil.currentUserId();

        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(60001, "消息不存在");
        }
        if (notification.getIsRead() == 1) {
            return;
        }

        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead() {
        Long userId = SecurityUtil.currentUserId();

        notificationMapper.update(
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, String title, String content, Integer type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);

        notificationMapper.insert(notification);

        log.debug("站内消息已发送: userId={} title={} type={}", userId, title, type);
    }

    // ==================== 内部方法 ====================

    /** Entity → VO */
    private NotificationVO toVO(Notification entity) {
        String typeText = switch (entity.getType()) {
            case 1 -> "系统通知";
            case 2 -> "订单通知";
            case 3 -> "退款通知";
            case 4 -> "到货提醒";
            default -> "未知类型";
        };

        return NotificationVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .typeText(typeText)
                .isRead(entity.getIsRead())
                .createTime(entity.getCreateTime())
                .build();
    }
}
