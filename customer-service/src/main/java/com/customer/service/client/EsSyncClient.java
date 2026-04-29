package com.customer.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign 客户端，通过 Nacos 发现 es-sync-service，调用其 ES 查询接口。
 */
@FeignClient(name = "es-sync-service")
public interface EsSyncClient {

    @PostMapping("/internal/es/page")
    CustomerNoResponse page(@RequestBody CustomerQueryRequest request);
}