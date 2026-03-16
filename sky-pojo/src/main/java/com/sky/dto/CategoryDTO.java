package com.sky.dto;

import jakarta.validation.constraints.*;

/**
 * 分类 DTO
 */
public record CategoryDTO(
        /** 主键 */
        Long id,
        
        /** 类型 1 菜品分类 2 套餐分类 */
        @NotNull(message = "分类类型不能为空")
        @Min(value = 1, message = "分类类型只能为 1 或 2")
        @Max(value = 2, message = "分类类型只能为 1 或 2")
        Integer type,
        
        /** 分类名称 */
        @NotBlank(message = "分类名称不能为空")
        @Size(min = 2, max = 32, message = "分类名称长度在 2-32 字符")
        String name,
        
        /** 排序 */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能为负数")
        @Max(value = 999, message = "排序值不能超过 999")
        Integer sort
) {}
