package com.customer.service.service;

import com.customer.service.client.EsSyncClient;
import com.customer.service.entity.Customer;
import com.customer.service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private EsSyncClient esSyncClient;

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(esSyncClient, customerRepository);
    }

    @Test
    void shouldGenerate400Inserts400Updates200Deletes() {
        // 已有 600 个 VA 前缀客户
        when(customerRepository.getMaxVaSeq()).thenReturn(10L);
        List<String> existingNos = IntStream.range(0, 600)
                .mapToObj(i -> String.format("VA00000000%d", i + 1))
                .toList();
        when(customerRepository.findActiveCustomerNos(600)).thenReturn(new ArrayList<>(existingNos));

        Map<String, Object> result = customerService.generateTestData();

        assertEquals(1000, result.get("total"));
        assertEquals(400, result.get("inserted"));
        assertEquals(400, result.get("updated"));
        assertEquals(200, result.get("deleted"));

        // 验证新增 400 条
        ArgumentCaptor<Customer> insertCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(400)).insert(insertCaptor.capture());
        List<Customer> inserted = insertCaptor.getAllValues();

        // 第一条：maxSeq(10) + 1 = 11 → VA0000000011
        assertEquals("VA0000000011", inserted.get(0).getCustomerNo());
        // 最后一条：maxSeq(10) + 400 = 410 → VA00000000410
        assertEquals("VA00000000410", inserted.get(399).getCustomerNo());
        // 验证格式：所有 customerNo 以 VA00000000 开头
        for (Customer c : inserted) {
            assertTrue(c.getCustomerNo().matches("VA00000000\\d+"),
                    "Expected VA00000000 format but got: " + c.getCustomerNo());
            assertEquals(0, c.getStatus());
            assertNotNull(c.getCreateTime());
            assertNotNull(c.getUpdateTime());
        }

        // 验证更新 400 条：updateByCustomerNo 被调用 400 次
        verify(customerRepository, times(400)).updateByCustomerNo(any(Customer.class));

        // 验证逻辑删除 200 条
        verify(customerRepository, times(200)).logicalDeleteByCustomerNo(anyString());
    }

    @Test
    void shouldHandleFewerExistingCustomersThanNeeded() {
        // 数据库只有 300 个有效客户，不够 400+200=600
        when(customerRepository.getMaxVaSeq()).thenReturn(0L);
        List<String> existingNos = IntStream.range(0, 300)
                .mapToObj(i -> String.format("VA00000000%d", i + 1))
                .toList();
        when(customerRepository.findActiveCustomerNos(600)).thenReturn(new ArrayList<>(existingNos));

        Map<String, Object> result = customerService.generateTestData();

        assertEquals(1000, result.get("total"));
        assertEquals(400, result.get("inserted"));
        // 只有 300 个现有客户，全用于更新，删除为 0
        assertEquals(300, result.get("updated"));
        assertEquals(0, result.get("deleted"));

        verify(customerRepository, times(400)).insert(any(Customer.class));
        verify(customerRepository, times(300)).updateByCustomerNo(any(Customer.class));
        verify(customerRepository, times(0)).logicalDeleteByCustomerNo(anyString());
    }

    @Test
    void shouldHandleZeroExistingCustomers() {
        // 数据库没有有效客户：只能新增
        when(customerRepository.getMaxVaSeq()).thenReturn(0L);
        when(customerRepository.findActiveCustomerNos(600)).thenReturn(List.of());

        Map<String, Object> result = customerService.generateTestData();

        assertEquals(400, result.get("inserted"));
        assertEquals(0, result.get("updated"));
        assertEquals(0, result.get("deleted"));

        verify(customerRepository, times(400)).insert(any(Customer.class));
        verify(customerRepository, never()).updateByCustomerNo(any(Customer.class));
        verify(customerRepository, never()).logicalDeleteByCustomerNo(anyString());
    }

    @Test
    void shouldStartFromSeq1WhenNoExistingVaCustomers() {
        when(customerRepository.getMaxVaSeq()).thenReturn(0L);
        when(customerRepository.findActiveCustomerNos(600)).thenReturn(List.of());

        Map<String, Object> result = customerService.generateTestData();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(400)).insert(captor.capture());
        assertEquals("VA000000001", captor.getAllValues().get(0).getCustomerNo());
    }

    @Test
    void shouldContinueSequenceFromExistingMax() {
        // 数据库已有 VA000000001 ~ VA000000099，maxSeq=99
        when(customerRepository.getMaxVaSeq()).thenReturn(99L);
        List<String> existingNos = IntStream.range(0, 600)
                .mapToObj(i -> String.format("VA00000000%d", i + 1))
                .toList();
        when(customerRepository.findActiveCustomerNos(600)).thenReturn(new ArrayList<>(existingNos));

        customerService.generateTestData();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(400)).insert(captor.capture());
        // 应该从 100 开始
        assertEquals("VA00000000100", captor.getAllValues().get(0).getCustomerNo());
        assertEquals("VA00000000499", captor.getAllValues().get(399).getCustomerNo());
    }
}