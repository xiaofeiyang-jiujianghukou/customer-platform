package com.customer.service.dto;

import java.util.List;

/**
 * 分页查询结果，包含完整客户数据。
 */
public class CustomerPageResponse {

    private List<com.customer.service.entity.Customer> data;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public CustomerPageResponse() {
    }

    public CustomerPageResponse(List<com.customer.service.entity.Customer> data, long total, int page, int size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    public List<com.customer.service.entity.Customer> getData() { return data; }
    public void setData(List<com.customer.service.entity.Customer> data) { this.data = data; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}