package com.sky.vo;

/**
 * 用户登录返回 VO（Java 25 Record）
 */
public record UserLoginVO(Long id, String openid, String token) {}
