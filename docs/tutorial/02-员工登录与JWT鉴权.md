# 02 - 员工登录与JWT鉴权

> 本教程将详细讲解登录流程和 JWT 鉴权机制

## 一、整体流程

```
┌─────────┐     POST /admin/employee/login      ┌─────────────┐
│  客户端  │ ──────────────────────────────────▶│  Controller │
└─────────┘                                      └──────┬──────┘
                                                      │
                                                      ▼
                                               ┌─────────────┐
                                               │   Service   │
                                               └──────┬──────┘
                                                      │
                                                      ▼
┌─────────┐     返回 token + 用户信息            ┌─────────────┐
│  客户端 │ ◀───────────────────────────────────│  Controller │
└─────────┘                                      └─────────────┘
```

## 二、核心文件

| 文件 | 作用 |
|------|------|
| `EmployeeController.java` | 处理登录请求 |
| `EmployeeService.java` | 验证账号密码 |
| `JwtUtil.java` | 生成和解析 JWT |
| `JwtTokenAdminInterceptor.java` | 拦截器校验 Token |
| `WebMvcConfiguration.java` | 注册拦截器 |

## 三、登录流程详解

### 3.1 Controller 层

**文件**: `sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java`

```java
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
    // 1. 调用 Service 验证登录
    Employee employee = employeeService.login(employeeLoginDTO);

    // 2. 生成 JWT 令牌
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.EMP_ID, employee.getId());  // 存入员工ID
    
    String token = JwtUtil.createJWT(
            jwtProperties.getAdminSecretKey(),  // 密钥
            jwtProperties.getAdminTtl(),         // 过期时间
            claims);

    // 3. 返回结果
    return Result.success(new EmployeeLoginVO(
            employee.getId(),
            employee.getUsername(),
            employee.getName(),
            token));
}
```

### 3.2 Service 层

**文件**: `sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java`

```java
public Employee login(EmployeeLoginDTO employeeLoginDTO) {
    // 1. 根据用户名查询数据库
    Employee employee = employeeMapper.getByUsername(employeeLoginDTO.getUsername());
    
    // 2. 判断是否存在
    if (employee == null) {
        throw new AccountNotFoundException("账号不存在");
    }
    
    // 3. 校验密码
    if (!employee.getPassword().equals(employeeLoginDTO.getPassword())) {
        throw new PasswordErrorException("密码错误");
    }
    
    // 4. 检查账号状态
    if (employee.getStatus() == 0) {
        throw new AccountDisableException("账号已禁用");
    }
    
    return employee;
}
```

### 3.3 JWT 配置

**文件**: `sky-server/src/main/resources/application.yml`

```yaml
sky:
  jwt:
    admin-secret-key: itcast              # 签名密钥（生产环境请使用复杂密钥）
    admin-ttl: 7200000                    # 过期时间（毫秒）= 2小时
    admin-token-name: token                # 请求头名称
```

## 四、JWT 详解

### 4.1 什么是 JWT？

JWT (JSON Web Token) 由三部分组成：
- **Header**: 头部（算法类型）
- **Payload**: 载荷（存放的业务数据）
- **Signature**: 签名（验证是否被篡改）

```
xxxxx.yyyyy.zzzzz
│     │     │
│     │     └─ Signature (签名)
│     └─ Payload (载荷)
└─ Header (头部)
```

### 4.2 生成 JWT

**文件**: `sky-common/src/main/java/com/sky/utils/JwtUtil.java`

```java
public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
    // 1. 生成密钥
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    
    // 2. 设置过期时间
    Date exp = new Date(System.currentTimeMillis() + ttlMillis);
    
    // 3. 生成 JWT
    return Jwts.builder()
            .claims(claims)           // 载荷
            .expiration(exp)          // 过期时间
            .signWith(key)            // 签名
            .compact();
}
```

### 4.3 解析 JWT

```java
public static Claims parseJWT(String secretKey, String token) {
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    
    return Jwts.parser()
            .verifyWith(key)              // 验证签名
            .build()
            .parseSignedClaims(token)      // 解析
            .getPayload();
}
```

## 五、鉴权拦截器

### 5.1 拦截器工作流程

```
请求进入 ──▶ 拦截器 preHandle ──▶ Controller ──▶ 拦截器 postHandle
              │                                              │
              │ 验证 Token                                    │
              │  成功 → 放行                                   │
              │  失败 → 返回 401                               │
              ▼                                              ▼
```

### 5.2 拦截器代码

**文件**: `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java`

```java
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 1. 只拦截 Controller 方法
    if (!(handler instanceof HandlerMethod)) {
        return true;
    }

    // 2. 从请求头获取 Token
    String token = request.getHeader("token");

    try {
        // 3. 解析 Token
        Claims claims = JwtUtil.parseJWT("itcast", token);
        Long empId = Long.valueOf(claims.get("empId").toString());
        
        // 4. 存入线程变量（方便后续获取当前用户）
        BaseContext.setCurrentId(empId);
        
        return true;  // 放行
    } catch (Exception e) {
        response.setStatus(401);  // 返回 401
        return false;            // 拦截
    }
}
```

### 5.3 注册拦截器

**文件**: `sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java`

```java
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    
    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")  // 拦截 /admin/* 路径
                .excludePathPatterns("/admin/employee/login");  // 排除登录
    }
}
```

## 六、实战练习

### 6.1 测试登录接口

使用 Postman 或 Knife4j 文档：

```bash
POST http://localhost:8080/admin/employee/login

# 请求体
{
    "username": "admin",
    "password": "123456"
}

# 响应
{
    "code": 1,
    "msg": "success",
    "data": {
        "id": 1,
        "username": "admin",
        "name": "管理员",
        "token": "eyJhbGciOiJIUzI1NiJ9..."
    }
}
```

### 6.2 测试鉴权

登录成功后，使用返回的 Token 访问其他接口：

```bash
GET http://localhost:8080/admin/employee

# 请求头
Authorization: token eyJhbGciOiJIUzI1NiJ9...
# 或
token: eyJhbGciOiJIUzI1NiJ9...
```

### 6.3 扩展：添加新员工

```bash
POST http://localhost:8080/admin/employee

# 请求头
token: <登录返回的token>

# 请求体
{
    "name": "新员工",
    "username": "zhangsan",
    "phone": "13800138000",
    "password": "123456",
    "sex": "男",
    "idNumber": "110101199001011234"
}
```

## 七、常见问题

### Q1: Token 过期怎么办？
- 前端收到 401 状态码，跳转到登录页
- 用户重新登录获取新 Token

### Q2: Token 被盗用怎么办？
- 短期 Token（如 2 小时）
- 后端可以维护 Token 黑名单

### Q3: 如何获取当前登录用户？
```java
// 在任意位置获取当前用户ID
Long empId = BaseContext.getCurrentId();
```

## 八、下一步

学完登录流程后，继续学习 **03 - 分类管理-CRUD**

---

> 📎 相关文件位置
> - Controller: `sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java`
> - Service: `sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java`
> - JWT工具: `sky-common/src/main/java/com/sky/utils/JwtUtil.java`
> - 拦截器: `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java`
