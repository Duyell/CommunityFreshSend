package com.duyell.communityfreshdelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>Knife4j / Swagger 接口文档配置</h2>
 *
 * <p>基于 <b>SpringDoc OpenAPI 3</b>，Knife4j 负责前端 UI 渲染.
 * 启动应用后访问 <a href="http://localhost:8080/doc.html">http://localhost:8080/doc.html</a> 即可浏览全部接口.</p>
 *
 * <h3>文档分组</h3>
 * <p>当前仅一个"全部接口"分组，扫描 {@code com.duyell.communityfreshdelivery} 下所有 Controller.
 * 后续可按端拆分（如用户端、商家端、配送端、管理端），各建一个 {@link GroupedOpenApi} Bean.</p>
 *
 * <h3>Controller 侧注解要求</h3>
 * <pre>{@code
 * @Tag(name = "用户模块")           // 模块名
 * public class UserController {
 *
 *     @Operation(summary = "查询用户信息")  // 接口描述
 *     @GetMapping("/{id}")
 *     public Result<UserVO> getById(@PathVariable Long id) { ... }
 * }
 * }</pre>
 *
 * <h3>yml 配套配置</h3>
 * <pre>{@code
 * knife4j:
 *   enable: true              # 启用 Knife4j UI
 *   setting:
 *     language: zh_cn         # 中文界面
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-01
 */
@Configuration
public class Knife4jConfig {

    /**
     * OpenAPI 文档元信息 — 显示在文档页顶部.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("社区生鲜配送系统")
                        .version("1.0.0")
                        .description("面向社区居民 + 生鲜仓库 + 配送人员 + 自提点的轻量级生鲜电商后端系统")
                        .contact(new Contact()
                                .name("duyell")
                                .email("duyell@example.com")));
    }

    /**
     * 接口分组 — 当前全量扫描，后续可按端拆分多个分组.
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("community-fresh")
                .displayName("全部接口")
                .packagesToScan("com.duyell.communityfreshdelivery")
                .build();
    }
}
