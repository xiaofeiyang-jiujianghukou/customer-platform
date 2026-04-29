package com.customer.service.repository;

import com.customer.service.client.EsSyncClient;
import com.customer.service.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false"
})
class GenerateTestDataIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        /**
         * 移除 @EnableFeignClients 自动注册的 EsSyncClient Bean 定义，
         * 然后注入 mock 替代。
         */
        @Bean
        static BeanFactoryPostProcessor removeFeignClientBeans() {
            return factory -> {
                if (factory instanceof BeanDefinitionRegistry registry) {
                    String feignBean = "com.customer.service.client.EsSyncClient";
                    if (registry.containsBeanDefinition(feignBean)) {
                        registry.removeBeanDefinition(feignBean);
                    }
                }
            };
        }

        @Bean
        EsSyncClient esSyncClient() {
            return mock(EsSyncClient.class);
        }
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM customer");
    }

    @Test
    @DisplayName("集成测试：空表 → 400新增 + 0更新 + 0删除，VA000000001 起")
    void emptyTableShouldInsert400() {
        Map<String, Object> result = customerService.generateTestData();

        assertEquals(1000, result.get("total"));
        assertEquals(400, result.get("inserted"));
        assertEquals(0, result.get("updated"));
        assertEquals(0, result.get("deleted"));

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer WHERE status = 0", Long.class);
        assertEquals(400L, count);

        String firstNo = jdbcTemplate.queryForObject(
                "SELECT customer_no FROM customer ORDER BY id LIMIT 1", String.class);
        assertEquals("VA000000001", firstNo);

        String lastNo = jdbcTemplate.queryForObject(
                "SELECT customer_no FROM customer ORDER BY id DESC LIMIT 1", String.class);
        assertEquals("VA00000000400", lastNo);

        List<String> nos = jdbcTemplate.queryForList(
                "SELECT customer_no FROM customer", String.class);
        for (String no : nos) {
            assertTrue(no.matches("VA00000000\\d+"), "Invalid format: " + no);
        }

        List<String> names = jdbcTemplate.queryForList(
                "SELECT name FROM customer WHERE name IS NULL OR name = ''", String.class);
        assertTrue(names.isEmpty(), "All customers should have a name");

        System.out.println("✓ 空表插入 400 条，customer_no=" + firstNo + " ~ " + lastNo);
    }

    @Test
    @DisplayName("集成测试：已有 600 条 VA 客户 → 400新增 + 400更新 + 200删除")
    void existingCustomersShouldUpdateAndDelete() {
        for (int i = 1; i <= 600; i++) {
            jdbcTemplate.update(
                    "INSERT INTO customer (customer_no, name, phone, source, channel, bu, product, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                    String.format("VA00000000%d", i),
                    "已有客户" + i, "13800000000", "官网", "直销", "BU1", "产品A"
            );
        }

        Map<String, Object> result = customerService.generateTestData();

        assertEquals(400, result.get("inserted"));
        assertEquals(400, result.get("updated"));
        assertEquals(200, result.get("deleted"));

        String firstNewNo = jdbcTemplate.queryForObject(
                "SELECT customer_no FROM customer WHERE name LIKE '测试用户%' ORDER BY id LIMIT 1",
                String.class);
        assertEquals("VA00000000601", firstNewNo);

        Long activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE status = 0", Long.class);
        assertEquals(800L, activeCount);

        Long deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE status = 1", Long.class);
        assertEquals(200L, deletedCount);

        Long updatedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE name LIKE '更新用户%'", Long.class);
        assertEquals(400L, updatedCount);

        System.out.printf("✓ 新增=%d, 更新=%d, 删除=%d, 有效=%d, 已删=%d%n",
                result.get("inserted"), result.get("updated"), result.get("deleted"),
                activeCount, deletedCount);
    }

    @Test
    @DisplayName("集成测试：连续调用验证序号递增")
    void consecutiveCallsShouldIncrementSequence() {
        customerService.generateTestData();
        Map<String, Object> result2 = customerService.generateTestData();

        assertEquals(400, result2.get("inserted"));
        assertEquals(400, result2.get("updated"));
        assertEquals(200, result2.get("deleted"));

        String maxNo = jdbcTemplate.queryForObject(
                "SELECT MAX(customer_no) FROM customer WHERE customer_no LIKE 'VA%'", String.class);
        assertNotNull(maxNo);
        assertTrue(maxNo.compareTo("VA00000000400") > 0, "第二轮序号应 > VA00000000400, 实际: " + maxNo);
        assertTrue(maxNo.compareTo("VA00000000900") < 0, "第二轮序号应 < VA00000000900, 实际: " + maxNo);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Long.class);
        System.out.printf("✓ 连续调用：总记录=%d, maxNo=%s%n", total, maxNo);
    }
}