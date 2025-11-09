//package com.sky.controller.user;
//
//import com.sky.result.Result;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.web.bind.annotation.*;
//
//@RestController("userShopController")
//@Slf4j
//@RequestMapping("/user/shop")
//public class ShopController {
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    //查询营业状态
//    @GetMapping("/status")
//    public Result<Integer> getStatus(){
//        Integer status= redisTemplate.opsForValue().get("SHOP_STATUS");
//        log.info("获取到店铺的营业状态{}",status==1?"营业中":"打烊中");
//        return Result.success(status);
//    }
//
//
//}

package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate; // 使用 StringRedisTemplate
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@RequestMapping("/user/shop")
public class ShopController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 注入 StringRedisTemplate

    /**
     * 设置营业状态
     */
    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result setStatus(@PathVariable Integer status) {
        // 校验参数
        if (status != 0 && status != 1) {
            return Result.error("状态值不合法：必须为 0 或 1");
        }

        log.info("设置营业状态为: {}", status == 1 ? "营业中" : "打烊中");

        // ✅ 正确：将 status 的值转成字符串存入 Redis
        stringRedisTemplate.opsForValue().set("SHOP_STATUS", status.toString());

        return Result.success();
    }

    /**
     * 查询营业状态
     */
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        String statusStr = stringRedisTemplate.opsForValue().get("SHOP_STATUS");
        Integer status = statusStr != null ? Integer.parseInt(statusStr) : 0;
        log.info("获取到店铺的营业状态: {}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
