package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>站内消息 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
