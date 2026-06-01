package com.duyell.communityfreshdelivery;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest
class EnvironmentCheckTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LettuceConnectionFactory redisConnectionFactory;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Value("${app.oss.endpoint}")
    private String ossEndpoint;

    @Value("${app.oss.access-key-id}")
    private String ossAccessKeyId;

    @Value("${app.oss.access-key-secret}")
    private String ossAccessKeySecret;

    @Value("${app.oss.bucket-name}")
    private String ossBucketName;

    private int passed = 0;
    private int failed = 0;

    @Test
    void checkAll() {
        System.out.println("\n========== 环境连通性检查 ==========\n");

        checkMySQL();
        checkRedis();
        checkRabbitMQ();
        checkOSS();

        System.out.println("\n========== 结果: " + (passed + failed) + " 项检查, "
                + passed + " 通过, " + failed + " 失败 ==========\n");

        if (failed > 0) {
            throw new RuntimeException("环境检查未通过，请确认外部服务已启动");
        }
    }

    void checkMySQL() {
        System.out.print("[MySQL]  连接 " + dataSource + " ... ");
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("√ 成功 (" + conn.getMetaData().getDatabaseProductVersion() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
            failed++;
        }
    }

    void checkRedis() {
        System.out.print("[Redis]  连接 " + redisConnectionFactory.getHostName()
                + ":" + redisConnectionFactory.getPort() + " ... ");
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            System.out.println("√ 成功 (" + pong + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
            failed++;
        }
    }

    void checkRabbitMQ() {
        System.out.print("[RabbitMQ] 连接 ... ");
        try {
            var conn = rabbitConnectionFactory.createConnection();
            System.out.println("√ 成功");
            conn.close();
            passed++;
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
            failed++;
        }
    }

    void checkOSS() {
        System.out.print("[OSS]    连接 " + ossEndpoint + " ... ");
        if (ossAccessKeyId.isBlank()) {
            System.out.println("- 跳过 (未配置 AccessKey)");
            return;
        }
        try {
            OSS client = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
            boolean exists = client.doesBucketExist(ossBucketName);
            System.out.println("√ 成功 (Bucket: " + ossBucketName + " " + (exists ? "存在" : "不存在") + ")");
            client.shutdown();
            passed++;
        } catch (Exception e) {
            System.out.println("✗ 失败: " + e.getMessage());
            failed++;
        }
    }
}
