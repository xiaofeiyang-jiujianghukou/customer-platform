package com.customer.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.customer.service.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 查询 VA 前缀客户编号的最大序号，无则返回 0。
     * 使用原生 SQL，不受 @TableLogic 影响，避免已删除记录的编号被复用。
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(customer_no, 11) AS UNSIGNED)), 0) FROM customer WHERE customer_no LIKE 'VA%'")
    long selectMaxVaSeq();
}