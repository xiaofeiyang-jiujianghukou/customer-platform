package com.customer.es.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String customerNo;
    private String name;
    private String phone;
    private String source;
    private String channel;
    private String bu;
    private String product;
    private Long salesId;
    private Long plannerId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getBu() { return bu; }
    public void setBu(String bu) { this.bu = bu; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Long getSalesId() { return salesId; }
    public void setSalesId(Long salesId) { this.salesId = salesId; }
    public Long getPlannerId() { return plannerId; }
    public void setPlannerId(Long plannerId) { this.plannerId = plannerId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}