# 苍穹外卖 - 架构优化建议

> 本文档对项目现有架构进行分析，并提供优化建议

---

## 一、当前架构概览

### 1.1 项目结构

```
sky-take-out/
├── sky-common/          # 公共模块
│   ├── constant/         # 常量
│   ├── exception/        # 异常类
│   ├── properties/       # 配置属性
│   ├── result/           # 统一响应
│   └── utils/            # 工具类
├── sky-pojo/           # 实体类模块
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   └── vo/              # 视图对象
└── sky-server/         # 业务模块
    ├── annotation/      # 自定义注解
    ├── aspect/          # AOP切面
    ├── config/          # 配置类
    ├── controller/      # 控制层
    ├── handler/         # 异常处理
    ├── interceptor/     # 拦截器
    ├── mapper/          # 数据层
    ├── service/         # 业务层
    ├── task/            # 定时任务
    └── websocket/       # WebSocket
```

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 4.0 + Java 25 |
| 数据库 | MySQL 8.0 + Druid |
| 缓存 | Redis 7.x |
| ORM | MyBatis + MyBatis-Plus |
| 接口文档 | Knife4j 4.5 |
| 对象存储 | 阿里云 OSS |
| 支付 | 微信支付 |
| 其他 | JWT、Lombok、Apache POI |
| 部署 | Docker |

---

## 二、优化建议

### 2.1 分层架构优化

#### 问题
- Controller 承担了部分业务逻辑
- Service 层有些逻辑可以直接在 Mapper 中完成

**建议**：严格遵循 MVC 三层架构

```
Controller  ──> Service  ──> Mapper  ──> DB
   │             │            │
   │             │            └── 单一职责：只做数据增删改查
   │             │
   │             └── 业务逻辑：事务控制、复杂计算
   │
   └── 请求响应：参数校验、格式转换
```

---

### 2.2 模块拆分优化

#### 问题
- sky-server 模块过于庞大（56个Java文件）
- 所有业务都堆在一起

**建议**：按业务拆分微服务（或模块）

```
优化后结构：
├── sky-api              # API聚合层（可选）
├── sky-module-user      # 用户服务
├── sky-module-order     # 订单服务
├── sky-module-product   # 商品服务（菜品、套餐、分类）
├── sky-module-pay       # 支付服务
└── sky-module-admin    # 管理服务
```

---

### 2.3 配置管理优化

#### 问题
- 敏感信息直接写在配置中
- 多环境配置不够灵活

**建议**：

```yaml
# application.yml
spring:
  config:
    import: optional:file:./config/application-secret.yml

# application-secret.yml（不提交到Git）
sky:
  jwt:
    admin-secret-key: ${JWT_SECRET}
  datasource:
    password: ${DB_PASSWORD}
  alioss:
    access-key-id: ${OSS_KEY}
    access-key-secret: ${OSS_SECRET}
```

**优化点**：
- 使用配置中心（Nacos/Apollo）
- 敏感信息通过环境变量注入
- 添加配置加密

---

### 2.4 数据库优化

#### 问题
- 缺少索引
- 没有分表设计
- 连接池配置可能不合理

**建议**：

```sql
-- 1. 添加必要索引
ALTER TABLE orders ADD INDEX idx_user_id (user_id);
ALTER TABLE orders ADD INDEX idx_status (status);
ALTER TABLE orders ADD INDEX idx_order_time (order_time);
ALTER TABLE order_detail ADD INDEX idx_order_id (order_id);
ALTER TABLE shopping_cart ADD INDEX idx_user_id (user_id);

-- 2. 订单表按月分表（数据量大时）
-- 使用 ShardingSphere 或 MyCat

-- 3. 读写分离（后期）
-- 添加主从复制，分担读压力
```

**Druid 配置优化**：
```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
```

---

### 2.5 缓存优化

#### 问题
- 缓存使用不够规范
- 没有缓存穿透、击穿、雪崩保护

**建议**：

```java
@Service
public class DishServiceImpl implements DishService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String DISH_CACHE = "dish_cache";
    
    public List<Dish> list(Long categoryId) {
        String key = DISH_CACHE + ":" + categoryId;
        
        // 1. 尝试从缓存获取
        List<Dish> cached = (List<Dish>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，查询数据库
        List<Dish> dishes = dishMapper.list(categoryId);
        
        // 3. 放入缓存（添加空值保护 + 过期时间）
        if (dishes != null && !dishes.isEmpty()) {
            redisTemplate.opsForValue().set(key, dishes, 1, TimeUnit.HOURS);
        }
        
        return dishes;
    }
    
    // 更新时清理缓存
    @CacheEvict(value = DISH_CACHE, key = "#categoryId")
    public void saveWithFlavor(DishDTO dishDTO) { ... }
}
```

**缓存策略建议**：
| 数据类型 | 缓存策略 | TTL |
|---------|---------|-----|
| 菜品分类 | Cacheable | 2小时 |
| 菜品列表 | Cacheable | 1小时 |
| 套餐列表 | Cacheable | 1小时 |
| 购物车 | 不缓存 | - |
| 用户信息 | Cacheable | 30分钟 |

---

### 2.6 日志优化

#### 问题
- 日志级别不够细分
- 没有分布式日志追踪

**建议**：

```java
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    // 使用占位符，避免字符串拼接开销
    log.info("创建订单: orderId={}, userId={}, amount={}", 
             orderId, userId, amount);
    
    // 敏感信息脱敏
    log.info("用户登录: username={}, ip={}", 
             username, maskIp(request));
    
    // 关键操作日志
    @Slf4j
    public void submitOrder(OrdersSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 开始下单, 地址: {}", userId, dto.getAddressBookId());
        // ... 业务逻辑
        log.info("订单创建成功: orderNumber={}", orderNumber);
    }
    
    private String maskIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        // 脱敏处理
        return ip;
    }
}
```

**日志配置优化**：
```yaml
logging:
  level:
    root: WARN
    com.sky.mapper: DEBUG
    com.sky.service: INFO
    com.sky: WARN
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

### 2.7 API 设计优化

#### 问题
- 缺少版本控制
- 错误码不统一
- 缺少请求日志

**建议**：

```java
// 1. 统一错误码
public enum ErrorCode {
    // 通用错误 1xxx
    PARAM_ERROR(1001, "参数错误"),
    NOT_FOUND(1002, "资源不存在"),
    
    // 认证错误 2xxx
    UNAUTHORIZED(2001, "未登录"),
    TOKEN_EXPIRED(2002, "Token已过期"),
    
    // 业务错误 3xxx
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    STOCK_NOT_ENOUGH(3003, "库存不足");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

// 2. 统一响应增加错误码
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;
    
    public static <T> Result<T> error(ErrorCode error) {
        Result<T> result = new Result<>();
        result.code = error.getCode();
        result.msg = error.getMessage();
        result.timestamp = System.currentTimeMillis();
        return result;
    }
}
```

---

### 2.8 安全优化

#### 问题
- JWT 密钥过于简单
- 缺少接口限流
- 缺少请求验签

**建议**：

```java
// 1. JWT 密钥配置化
sky:
  jwt:
    admin-secret-key: ${JWT_SECRET:生成一个64位随机字符串}
    admin-ttl: 7200000
    # 生产环境至少 256 位

// 2. 接口限流
@Configuration
public class RateLimitConfiguration {
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        // 限流配置：每分钟 100 次
    }
}

// 3. 参数验签（支付回调）
public boolean verifySign(Map<String, String> params, String sign) {
    // 验签逻辑
}

// 4. 添加 IP 黑名单
```

---

### 2.9 代码质量优化

#### 问题
- 部分异常捕获不规范
- 缺少事务注释
- 代码注释不够详细

**建议**：

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class OrderServiceImpl implements OrderService {
    
    /**
     * 提交订单
     * 1. 校验购物车
     * 2. 校验地址
     * 3. 计算金额
     * 4. 生成订单号
     * 5. 保存订单和明细
     * 6. 清空购物车
     * 
     * @param dto 订单提交DTO
     * @return 订单提交结果
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO dto) {
        // 业务逻辑
    }
}
```

---

### 2.10 监控与运维

#### 建议添加：

```yaml
# 1. Spring Boot Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# 2. 集成 SkyWalking / Pinpoint
# 3. 集成 Prometheus + Grafana
# 4. 集成 ELK 日志收集
```

---

## 三、优化优先级

| 优先级 | 优化项 | 预期收益 |
|-------|--------|----------|
| 🔴 P0 | 配置敏感信息外置 | 安全性 |
| 🔴 P0 | 数据库添加索引 | 查询性能 |
| 🔴 P0 | 完善缓存逻辑 | 响应速度 |
| 🟠 P1 | 日志规范化 | 可维护性 |
| 🟠 P1 | 统一错误码 | 体验 |
| 🟠 P1 | 接口限流 | 稳定性 |
| 🟡 P2 | 模块拆分 | 可扩展性 |
| 🟡 P2 | 监控接入 | 可观测性 |
| 🟡 P2 | 代码注释 | 可维护性 |

---

## 四、总结

当前项目架构清晰，分模块合理，适合学习和中小型项目生产使用。主要优化方向：

1. **安全第一**：敏感信息、接口限流、JWT优化
2. **性能其次**：数据库索引、缓存优化
3. **可维护性**：日志规范、错误码统一、代码注释
4. **可扩展性**：模块拆分、配置中心（后期）

建议按照优先级逐步优化，不用一次性全部改造。

---

> 📎 文档版本：v1.1  
> 最后更新：2026-03-16
