package com.customer.es.controller;

import com.customer.es.dto.CustomerNoResponse;
import com.customer.es.dto.CustomerQueryRequest;
import com.customer.es.service.EsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/es")
public class EsQueryController {

    private static final Logger log = LoggerFactory.getLogger(EsQueryController.class);

    private final EsSyncService esSyncService;

    public EsQueryController(EsSyncService esSyncService) {
        this.esSyncService = esSyncService;
    }

    @PostMapping("/page")
    public CustomerNoResponse page(@RequestBody CustomerQueryRequest request) {
        long start = System.currentTimeMillis();
        CustomerNoResponse response = esSyncService.search(request);
        log.info("[es-sync-service] [INFO] ES查询完成: total:{}, 耗时:{}ms",
                response.getTotal(), System.currentTimeMillis() - start);
        return response;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}