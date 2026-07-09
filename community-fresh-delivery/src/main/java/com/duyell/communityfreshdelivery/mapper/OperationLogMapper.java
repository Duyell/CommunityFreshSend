package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>操作日志 Mapper</h2>
 *
 * <p>继承 {@link BaseMapper}，获得通用 CRUD 方法.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
