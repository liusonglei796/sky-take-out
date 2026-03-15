package com.sky.vo;

/**
 * 数据概览 VO（Java 25 Record）
 */
public record BusinessDataVO(
        /** 营业额 */
        Double turnover,
        /** 有效订单数 */
        Integer validOrderCount,
        /** 订单完成率 */
        Double orderCompletionRate,
        /** 平均客单价 */
        Double unitPrice,
        /** 新增用户数 */
        Integer newUsers
) {}
