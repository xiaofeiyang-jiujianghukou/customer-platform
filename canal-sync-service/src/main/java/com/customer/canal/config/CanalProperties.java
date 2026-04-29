package com.customer.canal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "canal")
public class CanalProperties {

    private String host = "localhost";
    private int port = 11111;
    private String destination = "example";
    private String username = "";
    private String password = "";
    private String subscribe = ".*\\..*";
    private int batchSize = 1000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSubscribe() { return subscribe; }
    public void setSubscribe(String subscribe) { this.subscribe = subscribe; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
