package com.duyell.communityfreshdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <h2>社区生鲜配送系统 — 启动入口</h2>
 *
 * <p>Spring Boot 3.4.7 应用，启动后通过内嵌 Tomcat 监听 8080 端口提供服务.
 * {@link SpringBootApplication @SpringBootApplication} 自动开启组件扫描、自动配置与配置属性绑定.</p>
 *
 * <h3>技术栈</h3>
 * <ul>
 *   <li>Spring Boot 3.4.7 + MyBatis-Plus 3.5.9</li>
 *   <li>MySQL 8.0 + Redis + RabbitMQ 4.3</li>
 *   <li>Knife4j 4.5（Swagger 文档）</li>
 *   <li>Spring Security + JWT 认证</li>
 *   <li>阿里云 OSS 文件存储</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-05-31
 */
@EnableScheduling
@SpringBootApplication
public class CommunityFreshDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityFreshDeliveryApplication.class, args);
    }
}
