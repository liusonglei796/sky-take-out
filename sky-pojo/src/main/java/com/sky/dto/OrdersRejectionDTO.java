package com.sky.dto;

/**
 * 拒绝订单 DTO（Java 25 Record）
 */
public record OrdersRejectionDTO(
        Long id,
        /** 订单拒绝原因 */
        String rejectionReason
) {}
