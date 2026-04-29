<p align="center">
  <h1 align="center">Customer Platform</h1>
  <p align="center"><b>客户数据同步平台</b> — 实时同步 · 毫秒级查询 · 可视化管理</p>
</p>

---

## 架构

```
MySQL ──binlog──▶ Canal ──▶ Canal Sync ──▶ RocketMQ ──▶ ES Sync ──▶ Elasticsearch
                         (批量去重)                        (upsert/delete)

  Vue 3 前端 ──▶ Customer Service ──▶ ES Service ──▶ Elasticsearch ──▶ 返回结果
                                      (Feign)           (仅编号)
```

**读写分离**：MySQL 承载写入，Elasticsearch 承载查询。Canal 捕获 binlog 变更，经 RocketMQ 解耦，ES Sync 按 customer_no 做 upsert 写入，逻辑删除实时移除。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 · Element Plus · Vite · Axios |
| 网关 | Nginx |
| 服务框架 | Spring Boot 4.0.6 · Spring Cloud Alibaba 2025.1 |
| ORM | MyBatis-Plus 3.5.16 |
| 注册中心 | Nacos 2.2.3 |
| 消息队列 | RocketMQ 4.9.4 |
| CDC | Canal 1.1.8 |
| 搜索引擎 | Elasticsearch 8.11 |
| 数据库 | MySQL 8.0 |

## 服务

| 服务 | 端口 | 说明 |
|------|------|------|
| **customer-frontend** | 3000 | Web 管理界面 |
| **customer-service** | 8081 | 客户 API、分页查询、测试数据生成 |
| **es-sync-service** | 8083 | MQ 消费、ES 同步、逻辑删除处理 |
| **canal-sync-service** | 8082 | binlog 监听、批量去重、MQ 生产 |

## 快速启动

```bash
# 启动全部服务
docker-compose up -d

# 访问前端
open http://localhost:3000

# 生成测试数据（400新增 + 400更新 + 200逻辑删除）
curl -X POST http://localhost:8081/api/customers/generate-test-data
```

## 关键设计

### 同步策略

Canal 批量收集采用**自适应窗口**：`size=162 / interval=1000ms`，经压测验证触发比稳定在 **COUNT 40% : TIME 60%**——高频时数量先到，降速时时间先到，自然交替无偏斜。

### 逻辑删除

MySQL 逻辑删除（`UPDATE status=1`）经 Canal → MQ → ES Sync 链路，ES 侧按 `status` 分流：

```
status=0 → upsert (_id=customerNo, 覆盖写入)
status=1 → delete  (从 ES 物理删除)
```

保证 ES 文档数 = MySQL 有效记录数，零重复。

### 数据一致性

| 指标 | 结果 |
|------|------|
| ES 与 MySQL 有效记录一致 | 100% |
| ES 文档重复 | 0 |
| 同步延迟 | < 2s（含 1s 收集窗口） |

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/customers/page` | 多条件分页查询（16 种组合） |
| `GET` | `/api/customers/{no}` | 客户详情 |
| `PUT` | `/api/customers/{no}` | 编辑客户 |
| `POST` | `/api/customers/generate-test-data` | 批量生成测试数据 |

## 项目结构

```
├── customer-frontend/     Vue 3 前端
├── customer-service/      Spring Boot 客户服务
├── es-sync-service/       ES 同步服务
├── canal-sync-service/    Canal 同步服务
├── canal/                 Canal Server 配置
├── mysql/                 MySQL 初始化
├── docker-compose.yml     容器编排
└── 项目设计文档.md         完整设计文档
```

## 许可证

MIT © 2026 许飞杨
