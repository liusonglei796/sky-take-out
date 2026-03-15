package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 员工登录 DTO（Java 25 Record）
 */
@Schema(description = "员工登录时传递的数据模型")
public record EmployeeLoginDTO(
        @Schema(description = "用户名") String username,
        @Schema(description = "密码") String password
) {}
