package com.customer.es;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EsSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsSyncApplication.class, args);
    }
}