package com.customer.es.repository;

import com.customer.es.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomerRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_FULL =
            "SELECT id, customer_no, name, phone, source, channel, bu, product, " +
            "sales_id, planner_id, status, create_time, update_time " +
            "FROM customer WHERE customer_no = ?";

    private static final String SELECT_BY_NOS =
            "SELECT id, customer_no, name, phone, source, channel, bu, product, " +
            "sales_id, planner_id, status, create_time, update_time " +
            "FROM customer WHERE customer_no IN (%s)";

    public Customer findByCustomerNo(String customerNo) {
        List<Customer> list = jdbcTemplate.query(SELECT_FULL, this::mapRow, customerNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Customer> findByCustomerNos(List<String> customerNos) {
        if (customerNos == null || customerNos.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", customerNos.stream().map(n -> "?").toArray(String[]::new));
        String sql = String.format(SELECT_BY_NOS, placeholders);
        return jdbcTemplate.query(sql, this::mapRow, customerNos.toArray());
    }

    private Customer mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setCustomerNo(rs.getString("customer_no"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setSource(rs.getString("source"));
        c.setChannel(rs.getString("channel"));
        c.setBu(rs.getString("bu"));
        c.setProduct(rs.getString("product"));
        c.setSalesId(rs.getObject("sales_id", Long.class));
        c.setPlannerId(rs.getObject("planner_id", Long.class));
        c.setStatus(rs.getObject("status", Integer.class));
        c.setCreateTime(rs.getObject("create_time", java.time.LocalDateTime.class));
        c.setUpdateTime(rs.getObject("update_time", java.time.LocalDateTime.class));
        return c;
    }
}