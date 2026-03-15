package com.sky.dto;

/**
 * 确认订单 DTO（Java 25 Record）
 */
public record OrdersConfirmDTO(
        Long id,
        /** 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款 */
        Integer status
) {}
