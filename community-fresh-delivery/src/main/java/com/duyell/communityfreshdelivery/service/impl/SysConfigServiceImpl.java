package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.entity.SysConfig;
import com.duyell.communityfreshdelivery.mapper.SysConfigMapper;
import com.duyell.communityfreshdelivery.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>系统配置服务实现</h2>
 *
 * <p>首次读取时从数据库全量加载到内存缓存 {@code ConcurrentHashMap}，
 * 后续读取直接走缓存。管理员修改配置后需调用 {@link #refreshCache()} 刷新.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /** 内存缓存：configKey → configValue */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /** 缓存是否已初始化 */
    private volatile boolean loaded = false;

    public SysConfigServiceImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    // ==================== 读取 ====================

    @Override
    public String getString(String key, String defaultValue) {
        ensureLoaded();
        String value = cache.get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    @Override
    public BigDecimal getDecimal(String key, String defaultValue) {
        String raw = getString(key, defaultValue);
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("配置 {}={} 无法转为数值，使用默认值 {}", key, raw, defaultValue);
            return new BigDecimal(defaultValue);
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String raw = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("配置 {}={} 无法转为整数，使用默认值 {}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    // ==================== 管理 ====================

    @Override
    public Map<String, String> listAll() {
        ensureLoaded();
        // 按加载顺序返回（LinkedHashMap）
        Map<String, String> result = new LinkedHashMap<>();
        List<SysConfig> all = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId)
        );
        for (SysConfig config : all) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String key, String value) {
        SysConfig exist = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (exist == null) {
            // 新增配置项
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("");
            sysConfigMapper.insert(config);
            log.info("新增系统配置: {}={}", key, value);
        } else {
            exist.setConfigValue(value);
            sysConfigMapper.updateById(exist);
            log.info("更新系统配置: {}={}", key, value);
        }

        // 同步更新缓存
        cache.put(key, value);
    }

    @Override
    public void refreshCache() {
        cache.clear();
        loaded = false;
        ensureLoaded();
        log.info("系统配置缓存已刷新，当前 {} 项", cache.size());
    }

    // ==================== 内部方法 ====================

    /** 首次加载：从 DB 全量读入缓存 */
    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (loaded) {
                return;
            }
            List<SysConfig> all = sysConfigMapper.selectList(null);
            for (SysConfig config : all) {
                cache.put(config.getConfigKey(), config.getConfigValue());
            }
            loaded = true;
            log.info("系统配置加载完成: {} 项", cache.size());
        }
    }
}
