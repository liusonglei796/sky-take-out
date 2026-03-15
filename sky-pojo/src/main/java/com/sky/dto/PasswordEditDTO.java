package com.sky.dto;

/**
 * 修改密码 DTO（Java 25 Record）
 */
public record PasswordEditDTO(
        /** 员工id */
        Long empId,
        /** 旧密码 */
        String oldPassword,
        /** 新密码 */
        String newPassword
) {}
