package com.sky.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交订单返回 VO（Java 25 Record）
 */
public record OrderSubmitVO(
        /** 订单id */
        Long id,
        /** 订单号 */
        String orderNumber,
        /** 订单金额 */
        BigDecimal orderAmount,
        /** 下单时间 */
        LocalDateTime orderTime
) {}
