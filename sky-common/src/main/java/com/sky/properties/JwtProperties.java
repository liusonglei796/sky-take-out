package com.sky.properties;

import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    @NonNull
    private String adminSecretKey;
    private long adminTtl;
    @NonNull
    private String adminTokenName;

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    @NonNull
    private String userSecretKey;
    private long userTtl;
    @NonNull
    private String userTokenName;

}
