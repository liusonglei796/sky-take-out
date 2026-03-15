# Requirements Document

## Introduction

将 sky-take-out（苍穹外卖）项目从 Java 17 + Spring Boot 3.4.3 重构升级至 Java 25 + Spring Boot 4.0.x。
本次重构目标是充分利用 Java 25 LTS 的新语言特性（虚拟线程成熟化、Record、密封类、模式匹配等）和 Spring Boot 4.0 的新能力（模块化、API 版本控制、HTTP Service Clients、JSpecify 空安全等），在保持业务逻辑不变的前提下，提升代码质量、可维护性和运行时性能。

## Alignment with Product Vision

苍穹外卖是一个面向餐饮行业的全栈外卖平台，后端提供管理端（Admin）和用户端（User）REST API，并集成微信支付、阿里云 OSS、WebSocket 等第三方服务。本次重构不改变任何业务功能，仅升级技术底座。

---

## Requirements

### Requirement 1 — 升级构建环境与依赖

**User Story:** 作为开发者，我希望将 Maven 父 POM 升级到 Java 25 + Spring Boot 4.0.x，以便获得最新框架支持与安全补丁。

#### Acceptance Criteria

1. WHEN 构建项目 THEN 系统 SHALL 使用 Java 25（`<java.version>25</java.version>`）且编译无错误
2. WHEN 构建项目 THEN 系统 SHALL 使用 Spring Boot 4.0.x 父 POM
3. WHEN 运行 `mvn dependency:tree` THEN 系统 SHALL 不包含任何 `javax.*` 依赖（全部替换为 `jakarta.*`）
4. IF 存在已废弃的 Spring Boot 3.x 依赖 THEN 系统 SHALL 替换为 Spring Boot 4.0 对应的新 starter
5. WHEN 构建完成 THEN 系统 SHALL 通过所有现有单元测试

---

### Requirement 2 — 替换 ORM 层（MyBatis → Spring Data JPA / MyBatis-Plus 4.x）

**User Story:** 作为开发者，我希望数据访问层与 Spring Boot 4.0 完全兼容，以便利用最新持久化特性。

#### Acceptance Criteria

1. WHEN 项目启动 THEN 系统 SHALL 使用兼容 Spring Boot 4.0 的 MyBatis Spring Boot Starter（4.x）或替换为 Spring Data JPA（Hibernate 7）
2. WHEN 执行数据库 CRUD 操作 THEN 系统 SHALL 返回与现有业务逻辑相同的结果
3. WHEN 使用分页查询 THEN 系统 SHALL 使用兼容 Spring Boot 4.0 的 PageHelper 或 Spring Data 分页方案
4. IF 使用 Druid 连接池 THEN 系统 SHALL 升级至兼容 Jakarta EE 11 的版本（druid-spring-boot-3-starter 升级至 Spring Boot 4 兼容版本）

---

### Requirement 3 — 使用 Java 25 语言新特性重构 POJO 层

**User Story:** 作为开发者，我希望用 Java 25 的 Record、密封类、模式匹配等特性重构 POJO，以便减少样板代码、提升类型安全。

#### Acceptance Criteria

1. WHEN 定义纯数据传输对象（DTO / VO）THEN 系统 SHALL 优先使用 `record` 替代 Lombok `@Data` 类（不含业务方法的 DTO）
2. WHEN 定义领域状态枚举或有限状态集 THEN 系统 SHALL 评估使用密封类（`sealed interface`）替代普通枚举
3. WHEN 进行类型判断分支 THEN 系统 SHALL 使用模式匹配（`instanceof` 模式、`switch` 模式匹配）替代强转
4. IF DTO/VO 需要可变性或继承 THEN 系统 SHALL 保留普通类，不强制转 Record
5. WHEN 编译 THEN 系统 SHALL 无任何 `@SuppressWarnings("unchecked")` 的非必要抑制警告

---

### Requirement 4 — 启用虚拟线程（Virtual Threads）提升并发性能

**User Story:** 作为运维人员，我希望服务器在高并发请求下消耗更少的操作系统线程，以便降低基础设施成本。

#### Acceptance Criteria

1. WHEN 配置 `spring.threads.virtual.enabled=true` THEN 系统 SHALL 使用虚拟线程处理 HTTP 请求（Tomcat/Undertow 替换为 Tomcat 11 虚拟线程模式）
2. WHEN WebSocket 或定时任务执行 THEN 系统 SHALL 不因虚拟线程 pinning 产生死锁（Java 25 已解决 synchronized pinning 问题）
3. WHEN 压测 100 并发请求 THEN 系统 SHALL 响应时间不劣化于原平台线程方案

---

### Requirement 5 — 升级 API 文档（Knife4j → SpringDoc OpenAPI 3 / Knife4j 4.x for Spring Boot 4）

**User Story:** 作为前端开发者，我希望在 Spring Boot 4.0 下仍能访问完整的 Swagger UI，以便查看和调试接口。

#### Acceptance Criteria

1. WHEN 访问 `/doc.html` 或 `/swagger-ui.html` THEN 系统 SHALL 正确展示所有管理端和用户端 API
2. WHEN Spring Boot 4.0 启动 THEN 系统 SHALL 使用兼容 Spring Boot 4.0 的 Knife4j（5.x）或 springdoc-openapi 3.x
3. WHEN 生成 OpenAPI JSON THEN 系统 SHALL 包含所有接口的请求/响应 Schema

---

### Requirement 6 — JWT 与安全组件升级

**User Story:** 作为安全工程师，我希望 JWT 库和拦截器兼容 Jakarta EE 11，以便维持现有的 Token 认证流程。

#### Acceptance Criteria

1. WHEN 用户登录 THEN 系统 SHALL 生成有效 JWT Token（迁移至 `io.jsonwebtoken:jjwt-api` 0.12.x 或 `com.auth0:java-jwt`）
2. WHEN 请求携带有效 Token THEN 系统 SHALL 通过拦截器校验并设置 `ThreadLocal` 上下文
3. WHEN 请求不携带 Token 或 Token 无效 THEN 系统 SHALL 返回 401 响应
4. IF 使用 `javax.servlet.*` 过滤器/拦截器 THEN 系统 SHALL 全部替换为 `jakarta.servlet.*`

---

### Requirement 7 — 第三方服务集成兼容性

**User Story:** 作为后端开发者，我希望阿里云 OSS、微信支付、Apache POI 等第三方集成在新技术栈下正常工作。

#### Acceptance Criteria

1. WHEN 上传文件 THEN 系统 SHALL 通过阿里云 OSS SDK（升级至兼容 Jakarta EE 的版本）成功存储
2. WHEN 发起微信支付 THEN 系统 SHALL 正常调用微信支付 API 并处理回调
3. WHEN 导出 Excel 报表 THEN 系统 SHALL 使用 Apache POI 5.x 生成正确的 xlsx 文件
4. IF 第三方 SDK 不兼容 Jakarta EE 11 THEN 系统 SHALL 通过适配层隔离依赖

---

### Requirement 8 — 空安全（Null Safety）改造

**User Story:** 作为开发者，我希望利用 Spring Boot 4.0 的 JSpecify 空安全注解，以便在编译期发现潜在的 NPE。

#### Acceptance Criteria

1. WHEN 编写新代码 THEN 系统 SHALL 对所有公共 API 参数和返回值标注 `@NonNull` / `@Nullable`（JSpecify）
2. WHEN 构建 THEN 系统 SHALL 无 IDE/编译器报告的空安全警告（核心 service 层）
3. IF 遗留代码不适合立即改造 THEN 系统 SHALL 标记 TODO 注释，不强制全量改造

---

## Non-Functional Requirements

### Code Architecture and Modularity
- **单一职责**：各模块（sky-common、sky-pojo、sky-server）职责边界清晰，不引入跨模块循环依赖
- **模块化设计**：充分利用 Spring Boot 4.0 模块化 starter，按需引入依赖
- **向后兼容**：数据库表结构和 API 接口路径/响应格式保持不变

### Performance
- 启用虚拟线程后，吞吐量应不低于原平台线程方案
- 启动时间不超过现有版本 120%

### Security
- 依赖库无已知高危 CVE（CVSS ≥ 7.0）
- JWT 签名算法保持 HS256 以上强度

### Reliability
- 所有现有功能冒烟测试通过率 100%
- 数据库迁移零数据丢失

### Usability
- 开发者可通过 `mvn spring-boot:run` 一键启动，无需额外配置
- API 文档 UI 在浏览器可正常访问
