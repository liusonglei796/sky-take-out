package com.sky.dto;

/**
 * 取消订单 DTO（Java 25 Record）
 */
public record OrdersCancelDTO(
        Long id,
        /** 订单取消原因 */
        String cancelReason
) {}
