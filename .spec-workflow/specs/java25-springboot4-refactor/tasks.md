# Tasks — Java 25 + Spring Boot 4.0 重构

---

- [x] 1. 升级根 pom.xml 依赖版本
  - 文件：`pom.xml`
  - 将 `spring-boot-starter-parent` 升级至 `4.0.x`
  - 将 `<java.version>` 改为 `25`
  - 替换 `jjwt 0.9.1` 为 `jjwt-api + jjwt-impl + jjwt-jackson 0.12.6`
  - 替换 `knife4j-openapi3-jakarta-spring-boot-starter 4.4.0` 为 `5.x`
  - 将 `commons-lang 2.6` 替换为 `commons-lang3 3.19.0`（groupId 也变化）
  - 删除 `springfox` 相关依赖（若有显式声明）
  - 升级 `mybatis.spring` 属性至 `4.0.1`
  - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务1，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java/Maven 构建工程师。任务：升级根 pom.xml，将 Spring Boot 版本改为 4.0.x、Java 版本改为 25，替换 jjwt 为 0.12.6 三件套，knife4j 升为 5.x，commons-lang 替换为 commons-lang3，mybatis-spring 升至 4.0.1。限制：不要改动业务代码，不要删除业务相关依赖（OSS、微信支付、POI）。成功标准：`mvn dependency:tree` 无编译错误，无 javax.* 遗留依赖。先将任务标记为进行中，完成后用 log-implementation 记录实现细节，再标记为完成。_

- [x] 2. 升级 JwtUtil（jjwt 0.12.x 新 API）
  - 文件：`sky-common/src/main/java/com/sky/utils/JwtUtil.java`
  - 用 `Keys.hmacShaKeyFor()` 替换 `SignatureAlgorithm.HS256`
  - 用 `Jwts.builder().claims()` 替换 `.setClaims()`
  - 用 `Jwts.parser().verifyWith(key).build().parseSignedClaims()` 替换旧 parser
  - 删除 `import io.jsonwebtoken.SignatureAlgorithm` 等废弃 import
  - _Requirements: 6.1_
  - _Leverage: sky-common/src/main/java/com/sky/utils/JwtUtil.java_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务2，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 安全开发工程师。任务：将 JwtUtil.java 从 jjwt 0.9.1 API 迁移至 0.12.6 API，createJWT 和 parseJWT 方法签名保持不变，内部实现替换。限制：不修改方法签名，保持 HS256 算法，确保现有拦截器调用无需改动。成功标准：单元测试验证生成的 token 可被正确解析，Claims 内容一致。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 3. 替换 WebMvcConfiguration（删除 Springfox，接入 Knife4j 5.x）
  - 文件：`sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java`
  - 文件：`sky-server/src/main/resources/application.yml`
  - 删除 `Docket @Bean` 及所有 `springfox.documentation.*` import
  - 将类改为实现 `WebMvcConfigurer`（而非继承 `WebMvcConfigurationSupport`）
  - 在 `application.yml` 中添加 Knife4j 5.x 配置块
  - 保留 `addInterceptors` 和 `addResourceHandlers` 方法不变
  - _Requirements: 5.1, 5.2_
  - _Leverage: sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务3，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Spring MVC 配置工程师。任务：重构 WebMvcConfiguration，删除 Springfox Docket Bean，改为 Knife4j 5.x yml 配置方式；类改为 implements WebMvcConfigurer 而非 extends WebMvcConfigurationSupport，保留拦截器和静态资源配置。限制：不得删除拦截器注册逻辑，不得修改拦截路径。成功标准：启动后 /doc.html 可访问，所有 controller 接口可在文档中看到。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 4. 替换 sky-pojo 中所有 DTO 的 Swagger2 注解为 OpenAPI3 注解
  - 文件：`sky-pojo/src/main/java/com/sky/dto/*.java`（约 20 个文件）
  - 将 `import io.swagger.annotations.ApiModel` → `import io.swagger.v3.oas.annotations.media.Schema`
  - 将 `@ApiModel(description="...")` → `@Schema(description="...")`
  - 将 `@ApiModelProperty("...")` → `@Schema(description="...")`
  - _Requirements: 5.3_
  - _Leverage: sky-pojo/src/main/java/com/sky/dto/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务4，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 重构工程师。任务：批量将 sky-pojo/dto 目录下所有 DTO 文件的 Swagger2 注解（@ApiModel、@ApiModelProperty）替换为 OpenAPI3 注解（@Schema）。限制：不修改字段名称、类型、业务含义，不改动 Lombok 注解。成功标准：所有 DTO 文件无 io.swagger.annotations 导入，编译通过。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 5. 替换 sky-pojo 中所有 VO 的 Swagger2 注解为 OpenAPI3 注解
  - 文件：`sky-pojo/src/main/java/com/sky/vo/*.java`（约 15 个文件）
  - 同任务4，将 `@ApiModel` / `@ApiModelProperty` 替换为 `@Schema`
  - _Requirements: 5.3_
  - _Leverage: sky-pojo/src/main/java/com/sky/vo/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务5，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 重构工程师。任务：批量将 sky-pojo/vo 目录下所有 VO 文件的 Swagger2 注解替换为 OpenAPI3 注解。限制：不修改字段定义。成功标准：所有 VO 文件无 io.swagger.annotations 导入，编译通过。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 6. 替换 sky-server Controller 中的 Swagger2 注解为 OpenAPI3 注解
  - 文件：`sky-server/src/main/java/com/sky/controller/**/*.java`（所有 controller）
  - `@Api(tags="...")` → `@Tag(name="...")`
  - `@ApiOperation("...")` → `@Operation(summary="...")`
  - `@ApiParam` → `@Parameter`（如有）
  - _Requirements: 5.3_
  - _Leverage: sky-server/src/main/java/com/sky/controller/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务6，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 重构工程师。任务：批量将所有 Controller 文件的 Swagger2 注解替换为 OpenAPI3 注解（@Api→@Tag，@ApiOperation→@Operation）。限制：不修改请求映射路径、方法签名、业务逻辑。成功标准：所有 Controller 无 io.swagger.annotations 导入，编译通过，文档 UI 展示正常。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 7. 将无状态纯数据 DTO 改造为 Java 25 Record
  - 文件：`sky-pojo/src/main/java/com/sky/dto/` 中满足条件的 DTO
  - 适用条件：无继承、无业务方法、字段不可变、不被 MyBatis 直接映射
  - 候选：`EmployeeLoginDTO`、`UserLoginDTO`、`PasswordEditDTO`、`CategoryDTO`、`ShoppingCartDTO`、`OrdersPaymentDTO`、`OrdersCancelDTO`、`OrdersConfirmDTO`、`OrdersRejectionDTO` 等
  - 保留为普通类：`OrdersDTO`、`DishDTO`、`SetmealDTO`（含复杂嵌套或 mutable 需求）
  - _Requirements: 3.1, 3.4_
  - _Leverage: sky-pojo/src/main/java/com/sky/dto/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务7，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 25 语言特性专家。任务：将满足条件（无继承、无业务方法、不被 MyBatis 直接映射结果集）的 DTO 改造为 Java 25 record，保留 @Schema 注解，删除 Lombok @Data。对于 OrdersDTO、DishDTO、SetmealDTO 等含嵌套列表字段的保留为普通类。限制：改造后接口行为不变，Jackson 序列化结果相同；record 构造参数顺序与原字段声明顺序一致。成功标准：编译通过，现有 Controller 调用无需修改，Spring MVC 参数绑定正常（@RequestBody）。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 8. 将无状态纯视图 VO 改造为 Java 25 Record
  - 文件：`sky-pojo/src/main/java/com/sky/vo/` 中满足条件的 VO
  - 候选：`EmployeeLoginVO`、`UserLoginVO`、`OrderSubmitVO`、`OrderPaymentVO`、`BusinessDataVO`、`DishOverViewVO`、`OrderOverViewVO`、`SetmealOverViewVO` 等
  - 保留为普通类：`OrderVO`（含多个嵌套列表）
  - _Requirements: 3.1, 3.4_
  - _Leverage: sky-pojo/src/main/java/com/sky/vo/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务8，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 25 语言特性专家。任务：将满足条件的 VO 改造为 Java 25 record，保留 @Schema 注解，删除 Lombok @Data/@Builder。对含复杂嵌套列表的 VO 保留普通类。限制：改造后 JSON 序列化字段名不变，Jackson 可正确序列化 record。成功标准：编译通过，接口返回 JSON 结构与改造前一致。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 9. 启用虚拟线程并验证兼容性
  - 文件：`sky-server/src/main/resources/application.yml`
  - 添加 `spring.threads.virtual.enabled: true`
  - 检查 `WebSocketConfiguration`、`WebSocketServer` 使用的 `jakarta.websocket.*` 版本兼容性
  - 检查 `BaseContext`（ThreadLocal）在虚拟线程下行为（Spring Boot 4.0 兼容）
  - 检查定时任务 `OrderTask`、`WebSocketTask` 无 synchronized pinning 风险
  - _Requirements: 4.1, 4.2_
  - _Leverage: sky-server/src/main/resources/application.yml, sky-server/src/main/java/com/sky/websocket/WebSocketServer.java_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务9，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：JVM 并发工程师。任务：在 application.yml 中启用虚拟线程；审查 WebSocketServer、OrderTask、WebSocketTask 中是否有 synchronized 关键字（Java 25 JEP 491 已解决 pinning，但需记录）；确认 BaseContext 的 ThreadLocal 在 Spring Boot 4.0 虚拟线程模式下正常工作。限制：不修改业务逻辑，若发现 synchronized 仅记录注释不强制修改。成功标准：启动日志显示使用虚拟线程，基本功能冒烟测试通过。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 10. 升级 commons-lang 引用路径
  - 文件：全项目中所有 `import org.apache.commons.lang.` 的文件
  - 将 `org.apache.commons.lang.` → `org.apache.commons.lang3.`
  - _Requirements: 1.3_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务10，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 重构工程师。任务：全局搜索 `import org.apache.commons.lang.` 并替换为 `import org.apache.commons.lang3.`，确保 API 兼容（lang3 是 lang 的超集，绝大多数 API 兼容）。限制：只替换 import，不修改业务逻辑。成功标准：编译通过，无 lang 2.x 相关 import。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 11. 添加 JSpecify 空安全注解（核心层）
  - 文件：`sky-common/src/main/java/com/sky/properties/*.java`
  - 文件：`sky-server/src/main/java/com/sky/service/*.java`（接口方法签名）
  - 对非空参数/返回值标注 `@NonNull`，可空的标注 `@Nullable`
  - _Requirements: 8.1, 8.2_
  - _Leverage: sky-common/src/main/java/com/sky/properties/, sky-server/src/main/java/com/sky/service/_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务11，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：Java 代码质量工程师。任务：在 sky-common/properties 和 sky-server/service 接口层添加 JSpecify @NonNull/@Nullable 注解，重点覆盖 JwtProperties、AliOssProperties、WeChatProperties 属性类和各 Service 接口方法参数/返回值。限制：不修改实现逻辑，不强制改造 Mapper 层，遗留代码加 TODO 注释。成功标准：编译通过，核心 service 接口方法均有空安全标注。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_

- [x] 12. 全量构建验证与冒烟测试
  - 执行 `mvn clean package -DskipTests` 验证编译通过
  - 执行 `mvn test` 运行现有单元测试
  - 启动应用验证：管理端登录、菜品查询、订单查询
  - 验证 `/doc.html` Knife4j 文档正常展示
  - 检查启动日志确认虚拟线程已启用
  - _Requirements: 1.5, 4.3, 5.1_
  - _Prompt: 实现 spec java25-springboot4-refactor 的任务12，先运行 spec-workflow-guide 获取工作流指引，然后实现任务。角色：QA 测试工程师。任务：执行全量构建验证（mvn clean package），运行现有测试，启动应用执行冒烟测试（管理端登录/查询、文档页面、虚拟线程日志）。限制：发现编译错误需记录并修复（属于前序任务遗漏），不新增功能。成功标准：构建通过，测试通过，应用正常启动，文档页面可访问，虚拟线程启用确认。先将任务标记为进行中，完成后用 log-implementation 记录，再标记完成。_
