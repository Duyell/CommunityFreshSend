package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.NotificationVO;
import com.duyell.communityfreshdelivery.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <h2>站内消息接口</h2>
 *
 * <p>用户登录/刷新页面时拉取未读消息，支持已读标记和分页查看.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/notification")
@Tag(name = "站内消息", description = "站内消息通知")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数量")
    public Result<Map<String, Long>> unreadCount() {
        long count = notificationService.getUnreadCount();
        return Result.ok(Map.of("count", count));
    }

    @GetMapping("/unread")
    @Operation(summary = "未读消息列表")
    public Result<List<NotificationVO>> unread() {
        List<NotificationVO> list = notificationService.getUnread();
        return Result.ok(list);
    }

    @GetMapping("/page")
    @Operation(summary = "消息分页列表")
    public Result<Page<NotificationVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Page<NotificationVO> result = notificationService.page(page, size);
        return Result.ok(result);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }
}
