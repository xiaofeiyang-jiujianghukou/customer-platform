-- 创建 Canal 复制用户
CREATE USER 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;

-- 创建客户数据库
CREATE DATABASE IF NOT EXISTS customer_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE customer_db;

-- 创建客户表
CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_no VARCHAR(64) NOT NULL UNIQUE COMMENT '客户编号',
    name VARCHAR(128) DEFAULT '' COMMENT '客户名称',
    phone VARCHAR(32) DEFAULT '' COMMENT '电话',
    source VARCHAR(64) DEFAULT '' COMMENT '来源',
    channel VARCHAR(64) DEFAULT '' COMMENT '渠道',
    bu VARCHAR(64) DEFAULT '' COMMENT 'BU',
    product VARCHAR(64) DEFAULT '' COMMENT '产品',
    sales_id BIGINT DEFAULT NULL COMMENT '销售ID',
    planner_id BIGINT DEFAULT NULL COMMENT '规划师ID',
    status INT DEFAULT NULL COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_customer_no (customer_no),
    INDEX idx_sales_id (sales_id),
    INDEX idx_planner_id (planner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';