package com.customer.service.controller;

import com.customer.service.client.CustomerQueryRequest;
import com.customer.service.dto.CustomerPageResponse;
import com.customer.service.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.customer.service.entity.Customer;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 多条件组合分页查询客户数据。
     */
    @PostMapping("/page")
    public CustomerPageResponse page(@RequestBody CustomerQueryRequest request) {
        long start = System.currentTimeMillis();
        CustomerPageResponse response = customerService.page(request);
        log.info("[customer-service] [INFO] 接口耗时:{}ms, total:{}",
                System.currentTimeMillis() - start, response.getTotal());
        return response;
    }

    /**
     * 生成测试数据：1000条操作，40%新增、40%更新、20%逻辑删除。
     */
    @PostMapping("/generate-test-data")
    public Map<String, Object> generateTestData() {
        long start = System.currentTimeMillis();
        Map<String, Object> result = customerService.generateTestData();
        log.info("[customer-service] [INFO] generate-test-data 接口耗时:{}ms",
                System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 查询单个客户详情。
     */
    @GetMapping("/{customerNo}")
    public Customer getByCustomerNo(@PathVariable String customerNo) {
        return customerService.getByCustomerNo(customerNo);
    }

    /**
     * 编辑客户信息。
     */
    @PutMapping("/{customerNo}")
    public Map<String, Object> updateCustomer(@PathVariable String customerNo,
                                               @RequestBody Customer update) {
        customerService.updateCustomer(customerNo, update);
        return Map.of("success", true);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}