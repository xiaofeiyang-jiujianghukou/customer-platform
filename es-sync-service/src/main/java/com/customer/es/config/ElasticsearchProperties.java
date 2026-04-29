package com.customer.es.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    private String host = "localhost";
    private int port = 49200;
    private String index = "customer_index";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getIndex() { return index; }
    public void setIndex(String index) { this.index = index; }
}