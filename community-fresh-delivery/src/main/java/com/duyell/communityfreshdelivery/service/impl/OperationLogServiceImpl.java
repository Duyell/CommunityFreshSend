package com.duyell.communityfreshdelivery.service.impl;

import com.duyell.communityfreshdelivery.entity.OperationLog;
import com.duyell.communityfreshdelivery.mapper.OperationLogMapper;
import com.duyell.communityfreshdelivery.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>操作日志服务实现</h2>
 *
 * <p>使用独立事务 {@code REQUIRES_NEW}，确保日志写入不受主事务回滚影响.
 * 即使主业务失败，日志依然会保存下来供排查.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(Long userId, String action, String targetType, Long targetId,
                       String fromStatus, String toStatus, String detail) {
        OperationLog logEntity = new OperationLog();
        logEntity.setUserId(userId);
        logEntity.setAction(action);
        logEntity.setTargetType(targetType);
        logEntity.setTargetId(targetId);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setDetail(detail);

        operationLogMapper.insert(logEntity);

        log.debug("操作日志已记录: userId={} action={} targetType={} targetId={} {}→{}",
                userId, action, targetType, targetId, fromStatus, toStatus);
    }
}
