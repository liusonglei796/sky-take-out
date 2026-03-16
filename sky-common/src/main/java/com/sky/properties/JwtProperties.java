package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    private String adminSecretKey = "itcast";
    private long adminTtl;
    private String adminTokenName = "token";

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    private String userSecretKey = "itcast";
    private long userTtl;
    private String userTokenName = "userToken";

}
