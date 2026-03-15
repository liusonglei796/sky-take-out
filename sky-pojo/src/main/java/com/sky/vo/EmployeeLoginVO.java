package com.sky.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 员工登录返回 VO（Java 25 Record）
 */
@Schema(description = "员工登录返回的数据格式")
public record EmployeeLoginVO(
        @Schema(description = "主键值") Long id,
        @Schema(description = "用户名") String userName,
        @Schema(description = "姓名") String name,
        @Schema(description = "jwt令牌") String token
) {}
