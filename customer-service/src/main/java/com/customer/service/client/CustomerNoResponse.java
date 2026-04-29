package com.customer.service.client;

/**
 * Feign 调用 es-sync-service 的响应 DTO。
 * es-sync-service 返回 /internal/es/page 的 JSON 结构。
 */
public class CustomerNoResponse {

    private java.util.List<String> customerNos;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public java.util.List<String> getCustomerNos() { return customerNos; }
    public void setCustomerNos(java.util.List<String> customerNos) { this.customerNos = customerNos; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}