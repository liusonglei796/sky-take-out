package com.sky.dto;

/**
 * 购物车 DTO（Java 25 Record）
 */
public record ShoppingCartDTO(
        Long dishId,
        Long setmealId,
        String dishFlavor
) {}
