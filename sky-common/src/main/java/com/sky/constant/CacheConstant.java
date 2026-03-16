package com.sky.constant;

/**
 * 缓存常量类
 */
public class CacheConstant {
    /**
     * 菜品缓存前缀
     */
    public static final String DISH_CACHE_PREFIX = "dish_";

    /**
     * 套餐缓存前缀
     */
    public static final String SETMEAL_CACHE_PREFIX = "setmeal_";

    /**
     * 菜品缓存过期时间（秒）- 1小时
     */
    public static final long DISH_CACHE_EXPIRE_TIME = 3600L;

    /**
     * 套餐缓存过期时间（秒）- 1小时
     */
    public static final long SETMEAL_CACHE_EXPIRE_TIME = 3600L;

    /**
     * 用户信息缓存过期时间（秒）- 30分钟
     */
    public static final long USER_INFO_CACHE_EXPIRE_TIME = 1800L;
}
