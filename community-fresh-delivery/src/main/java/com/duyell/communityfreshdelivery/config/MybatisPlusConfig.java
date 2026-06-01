package com.duyell.communityfreshdelivery.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>MyBatis-Plus 配置</h2>
 *
 * <p>注册核心拦截器链，当前仅启用 <b>分页插件</b>.
 * 后续如需乐观锁、防全表更新/删除等，在本类追加 {@code addInnerInterceptor} 即可.</p>
 *
 * <h3>分页使用方式</h3>
 * <pre>{@code
 * // Service / Controller 层
 * Page<Product> page = new Page<>(1, 10);  // 第1页，每页10条
 * IPage<ProductVO> result = productMapper.selectPage(page, wrapper);
 * }</pre>
 *
 * <h3>依赖说明</h3>
 * <p>MyBatis-Plus 3.5.9 起，{@code PaginationInnerInterceptor} 拆分到独立模块
 * {@code mybatis-plus-jsqlparser}，需在 pom.xml 中单独引入.</p>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链.
     * <ul>
     *   <li>分页插件 — 识别 {@code Page} 参数后自动改写 SQL 追加 LIMIT</li>
     *   <li>数据库方言 — MySQL，生成 {@code LIMIT offset, size} 格式</li>
     * </ul>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件 — 必须放在最后（官方建议）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
