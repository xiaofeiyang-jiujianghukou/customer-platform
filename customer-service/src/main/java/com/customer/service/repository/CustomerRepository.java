package com.customer.service.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.customer.service.entity.Customer;
import com.customer.service.mapper.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomerRepository.class);

    private final CustomerMapper customerMapper;

    public CustomerRepository(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public Customer findByCustomerNo(String customerNo) {
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerNo, customerNo));
    }

    public List<Customer> findByCustomerNos(List<String> customerNos) {
        if (customerNos == null || customerNos.isEmpty()) {
            return List.of();
        }
        return customerMapper.selectList(
                new LambdaQueryWrapper<Customer>().in(Customer::getCustomerNo, customerNos));
    }

    /**
     * 查询指定数量的有效客户编号（未被逻辑删除的）。
     */
    public List<String> findActiveCustomerNos(int limit) {
        List<Customer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<Customer>()
                        .select(Customer::getCustomerNo)
                        .last("LIMIT " + limit));
        return new ArrayList<>(customers.stream().map(Customer::getCustomerNo).toList());
    }

    /**
     * 获取 VA 前缀的最大序号，没有则返回 0。
     */
    public long getMaxVaSeq() {
        return customerMapper.selectMaxVaSeq();
    }

    public void insert(Customer customer) {
        customerMapper.insert(customer);
    }

    public void updateByCustomerNo(Customer customer) {
        customerMapper.update(customer,
                new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerNo, customer.getCustomerNo()));
    }

    /**
     * 逻辑删除：MyBatis-Plus 的 deleteById 配合 @TableLogic 会自动将 status 置为 1。
     */
    public void logicalDeleteByCustomerNo(String customerNo) {
        customerMapper.delete(new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerNo, customerNo));
    }
}