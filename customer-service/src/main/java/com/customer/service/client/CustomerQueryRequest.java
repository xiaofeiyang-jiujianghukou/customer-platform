package com.customer.service.client;

/**
 * ES 查询请求 DTO，与 es-sync-service 的 CustomerQueryRequest 结构一致。
 */
public class CustomerQueryRequest {

    /** 精确匹配 */
    private String customerNo;
    private Integer status;
    private Integer salesId;
    private Integer plannerId;
    private String source;
    private String channel;
    private String bu;
    private String product;

    /** 模糊匹配 */
    private String name;
    private String phone;

    /** 范围查询 */
    private Integer minId;
    private Integer maxId;
    private String startTime;
    private String endTime;

    /** 分页 */
    private int page = 1;
    private int size = 20;

    /** 排序 */
    private String sortField = "id";
    private String sortOrder = "desc";

    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSalesId() { return salesId; }
    public void setSalesId(Integer salesId) { this.salesId = salesId; }
    public Integer getPlannerId() { return plannerId; }
    public void setPlannerId(Integer plannerId) { this.plannerId = plannerId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getBu() { return bu; }
    public void setBu(String bu) { this.bu = bu; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getMinId() { return minId; }
    public void setMinId(Integer minId) { this.minId = minId; }
    public Integer getMaxId() { return maxId; }
    public void setMaxId(Integer maxId) { this.maxId = maxId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}