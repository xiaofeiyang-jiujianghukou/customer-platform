package com.customer.es.dto;

import java.util.List;

public class CustomerNoResponse {

    private List<String> customerNos;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public CustomerNoResponse() {
    }

    public CustomerNoResponse(List<String> customerNos, long total, int page, int size) {
        this.customerNos = customerNos;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    public List<String> getCustomerNos() { return customerNos; }
    public void setCustomerNos(List<String> customerNos) { this.customerNos = customerNos; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}