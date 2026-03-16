# 03 - 分类管理 CRUD

> 本教程讲解分类管理模块的增删改查操作

## 一、数据库表设计

### 1.1 分类表 (category)

```sql
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` int DEFAULT NULL COMMENT '类型: 1菜品分类 2套餐分类',
  `name` varchar(50) DEFAULT NULL COMMENT '分类名称',
  `sort` int DEFAULT NULL COMMENT '顺序',
  `status` int DEFAULT '0' COMMENT '分类状态 0禁用 1启用',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
);
```

### 1.2 实体类

**文件**: `sky-pojo/src/main/java/com/sky/entity/Category.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category implements Serializable {
    private Long id;
    private Integer type;      // 1=菜品分类, 2=套餐分类
    private String name;       // 分类名称
    private Integer sort;      // 排序
    private Integer status;    // 0=禁用, 1=启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
}
```

## 二、整体架构

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ Controller  │ ───▶ │  Service    │ ───▶ │   Mapper    │
│   控制层     │      │   业务层     │      │   数据层     │
└─────────────┘      └─────────────┘      └─────────────┘
```

### 2.1 核心文件

| 层级 | 文件 | 职责 |
|------|------|------|
| Controller | `CategoryController.java` | 接收请求，返回响应 |
| Service | `CategoryService.java` | 业务逻辑处理 |
| ServiceImpl | `CategoryServiceImpl.java` | 业务逻辑实现 |
| Mapper | `CategoryMapper.java` | 数据库操作 |
| Entity | `Category.java` | 数据实体 |

## 三、CRUD 完整流程

### 3.1 新增分类

**Controller**:
```java
@PostMapping
public Result<String> save(@RequestBody CategoryDTO categoryDTO) {
    categoryService.save(categoryDTO);
    return Result.success();
}
```

**Service**:
```java
public void save(CategoryDTO categoryDTO) {
    Category category = new Category();
    // DTO → Entity 转换
    category.setType(categoryDTO.type());
    category.setName(categoryDTO.name());
    category.setSort(categoryDTO.sort());
    
    // 默认禁用状态
    category.setStatus(StatusConstant.DISABLE);
    
    categoryMapper.insert(category);
}
```

**Mapper**:
```java
@Insert("INSERT INTO category (type, name, sort, status) VALUES (#{type}, #{name}, #{sort}, #{status})")
void insert(Category category);
```

**测试**:
```bash
POST /admin/category
Content-Type: application/json
token: <登录获取的token>

{
    "type": 1,
    "name": "热销菜品",
    "sort": 1
}
```

### 3.2 分页查询

**Controller**:
```java
@GetMapping("/page")
public Result<PageResult> page(CategoryPageQueryDTO dto) {
    PageResult result = categoryService.pageQuery(dto);
    return Result.success(result);
}
```

**Service**:
```java
public PageResult pageQuery(CategoryPageQueryDTO dto) {
    // PageHelper 分页插件
    PageHelper.startPage(dto.getPage(), dto.getPageSize());
    
    Page<Category> page = categoryMapper.pageQuery(dto);
    
    return new PageResult(page.getTotal(), page.getResult());
}
```

**Mapper**:
```java
Page<Category> pageQuery(CategoryPageQueryDTO dto);
```

**XML**:
```xml
<select id="pageQuery" resultType="Category">
    SELECT * FROM category
    <where>
        <if test="name != null">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="type != null">
            AND type = #{type}
        </if>
    </where>
    ORDER BY sort ASC, create_time DESC
</select>
```

**测试**:
```bash
GET /admin/category/page?page=1&pageSize=10&type=1
```

**响应**:
```json
{
    "code": 1,
    "data": {
        "total": 5,
        "records": [
            {"id": 1, "name": "热销菜品", "type": 1, "sort": 1, "status": 1}
        ]
    }
}
```

### 3.3 修改分类

**Controller**:
```java
@PutMapping
public Result<String> update(@RequestBody CategoryDTO categoryDTO) {
    categoryService.update(categoryDTO);
    return Result.success();
}
```

**Service**:
```java
public void update(CategoryDTO categoryDTO) {
    Category category = new Category();
    category.setId(categoryDTO.id());
    category.setName(categoryDTO.getName());
    category.setType(categoryDTO.getType());
    category.setSort(categoryDTO.getSort());
    
    categoryMapper.update(category);
}
```

**Mapper**:
```java
@Update("UPDATE category SET name=#{name}, type=#{type}, sort=#{sort} WHERE id=#{id}")
void update(Category category);
```

### 3.4 删除分类

**Controller**:
```java
@DeleteMapping
public Result<String> deleteById(Long id) {
    categoryService.deleteById(id);
    return Result.success();
}
```

**Service - 业务校验**:
```java
public void deleteById(Long id) {
    // 1. 检查是否关联了菜品
    Integer count = dishMapper.countByCategoryId(id);
    if (count > 0) {
        throw new DeletionNotAllowedException("分类下有菜品，不能删除");
    }
    
    // 2. 检查是否关联了套餐
    count = setmealMapper.countByCategoryId(id);
    if (count > 0) {
        throw new DeletionNotAllowedException("分类下有套餐，不能删除");
    }
    
    // 3. 执行删除
    categoryMapper.deleteById(id);
}
```

**测试**:
```bash
DELETE /admin/category?id=1
```

### 3.5 启用/禁用分类

**Controller**:
```java
@PostMapping("/status/{status}")
public Result<String> startOrStop(@PathVariable("status") Integer status, Long id) {
    categoryService.startOrStop(status, id);
    return Result.success();
}
```

**测试**:
```bash
# 启用
POST /admin/category/status/1?id=1

# 禁用
POST /admin/category/status/0?id=1
```

### 3.6 根据类型查询

**Controller**:
```java
@GetMapping("/list")
public Result<List<Category>> list(Integer type) {
    List<Category> list = categoryService.list(type);
    return Result.success(list);
}
```

**测试**:
```bash
# 查询所有菜品分类
GET /admin/category/list?type=1

# 查询所有套餐分类
GET /admin/category/list?type=2
```

## 四、统一响应格式

**文件**: `sky-common/src/main/java/com/sky/result/Result.java`

```java
@Data
public class Result<T> {
    private Integer code;      // 1=成功, 0=失败
    private String msg;        // 提示信息
    private T data;            // 返回数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
```

**统一响应示例**:
```json
// 成功
{"code": 1, "msg": "success", "data": {}}

// 失败
{"code": 0, "msg": "分类下有菜品，不能删除", "data": null}
```

## 五、分页实现原理

### 5.1 PageHelper 原理

```
1. PageHelper.startPage(1, 10)
   ↓ 设置 ThreadLocal 分页参数
2. 执行 Mapper 查询
   ↓ 自动在 SQL 末尾添加 LIMIT
3. 返回 Page 对象
   ↓ 包含 total 和 records
4. 封装 PageResult
```

### 5.2 DTO vs VO vs Entity

| 缩写 | 全称 | 用途 | 示例 |
|------|------|------|------|
| DTO | Data Transfer Object | 接收请求参数 | `CategoryDTO` |
| Entity | Entity | 数据库实体 | `Category` |
| VO | View Object | 返回视图数据 | `CategoryVO` |

## 六、实战练习

### 练习1：新增一个"特色菜"分类
```bash
POST /admin/category
{
    "type": 1,
    "name": "特色菜",
    "sort": 2
}
```

### 练习2：查询所有启用的菜品分类
```bash
GET /admin/category/list?type=1
```

### 练习3：修改分类名称
```bash
PUT /admin/category
{
    "id": 1,
    "name": "招牌菜",
    "type": 1,
    "sort": 1
}
```

## 七、常见问题

### Q1: 删除分类失败？
检查是否有关联的菜品或套餐，需要先删除关联数据

### Q2: 分页查询不到数据？
检查 `PageHelper.startPage()` 是否在查询之前调用

### Q3: 新增的分类显示不出来？
检查分类状态是否为启用(1)

## 八、下一步

继续学习 **04 - 菜品管理-文件上传**

---

> 📎 相关文件位置
> - Controller: `sky-server/src/main/java/com/sky/controller/admin/CategoryController.java`
> - Service: `sky-server/src/main/java/com/sky/service/impl/CategoryServiceImpl.java`
> - Mapper: `sky-server/src/main/java/com/sky/mapper/CategoryMapper.java`
> - Entity: `sky-pojo/src/main/java/com/sky/entity/Category.java`
> - Mapper XML: `sky-server/src/main/resources/mapper/CategoryMapper.xml`
