package com.customer.canal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CanalSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanalSyncApplication.class, args);
        // 保持主线程存活，CommandLineRunner 在非守护线程中运行 canal 轮询
    }
}