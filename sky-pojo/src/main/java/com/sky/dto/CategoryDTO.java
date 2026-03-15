package com.sky.dto;

/**
 * 分类 DTO（Java 25 Record）
 */
public record CategoryDTO(
        /** 主键 */
        Long id,
        /** 类型 1 菜品分类 2 套餐分类 */
        Integer type,
        /** 分类名称 */
        String name,
        /** 排序 */
        Integer sort
) {}
