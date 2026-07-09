package com.duyell.communityfreshdelivery.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * <h2>系统配置服务</h2>
 *
 * <p>统一的业务配置读取入口，所有 Service 通过本服务读取运行时配置，
 * 不再直接查 {@code sys_config} 表或用 {@code @Value} 注入.</p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>内存缓存：首次读取时全量加载，修改后调用 {@link #refreshCache()} 刷新</li>
 *   <li>类型安全：提供 getString / getDecimal / getInt 三个方法，传默认值</li>
 *   <li>管理接口：listAll / update / refreshCache 供管理员后台使用</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface SysConfigService {

    /**
     * 获取字符串配置.
     *
     * @param key          配置键
     * @param defaultValue 默认值（配置不存在时返回）
     */
    String getString(String key, String defaultValue);

    /**
     * 获取数值配置.
     *
     * @param key          配置键
     * @param defaultValue 默认值
     */
    BigDecimal getDecimal(String key, String defaultValue);

    /**
     * 获取整数配置.
     *
     * @param key          配置键
     * @param defaultValue 默认值
     */
    int getInt(String key, int defaultValue);

    /**
     * 列出所有配置（管理员接口用）.
     *
     * @return key → value 映射
     */
    Map<String, String> listAll();

    /**
     * 更新配置值（管理员接口用）.
     *
     * @param key   配置键
     * @param value 新值
     */
    void update(String key, String value);

    /**
     * 刷新缓存（修改配置后调用）.
     */
    void refreshCache();
}
