//package com.sky.aspect;
//
//import com.sky.annotation.AutoFill;
//import com.sky.context.BaseContext;
//import com.sky.enumeration.OperationType;
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.aspectj.lang.annotation.Pointcut;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.stereotype.Component;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.time.LocalDateTime;
//
////自定义切面，实现公共字段自动填充处理逻辑
//@Aspect
//@Component
//@Slf4j
//public class AutoFillAspect {
//    //切入点
//    @Pointcut("execution(* com.sky.mapper.*.*(..))&&@annotation(com.sky.annotation.AutoFill)")
//    public void autoFillPointCut(){}
//    //前置通知
//    @Before("autoFillPointCut()")
//    public void autoFill(JoinPoint joinPoint){
//        log.info("开始进行公共字段填充...");
//
//        //获取当前被拦截的方法上的数据库操作类型
//        MethodSignature signature=(MethodSignature) joinPoint.getSignature(); //方法签名对象
//        AutoFill autoFill=signature.getMethod().getAnnotation(AutoFill.class); //获取方法上的注解对象
//        OperationType operationType=autoFill.value();    //获取数据库操作类型
//
//        //获取到当前被拦截的方法的参数--实体对象
//        Object[] args=joinPoint.getArgs();
//        if(args==null||args.length==0){
//            return;
//        }
//         Object entity= args[0];
//        //准备赋值的数据
//        LocalDateTime now=LocalDateTime.now();
//        Long currentId= BaseContext.getCurrentId();
//        //根据当前不同的操作类型，为对应的属性通过反射来赋值
//        if(operationType== OperationType.INSERT){
//            //为4个公共字段赋值
//            try {
//                Method setCreateTime= entity.getClass().getMethod("setUpdateTime",LocalDateTime.class);
//                Method setCreateUser= entity.getClass().getMethod("setCreateUser",Long.class);
//                Method setUpdateTime= entity.getClass().getMethod("setUpdateTime",LocalDateTime.class);
//                Method setUpdateUser= entity.getClass().getMethod("setUpdateUser",Long.class);
//                //通过反射为对象属性赋值
//                setCreateTime.invoke(entity,now);
//                setCreateUser.invoke(entity,currentId);
//                setUpdateTime.invoke(entity,now);
//                setUpdateUser.invoke(entity,currentId);
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//        }else if(operationType== OperationType.UPDATE){
//            //为两个字段赋值
//            Method setUpdateTime= entity.getClass().getMethod("setUpdateTime",LocalDateTime.class);
//            Method setUpdateUser= entity.getClass().getMethod("setUpdateUser",Long.class);
//            //通过反射为对象属性赋值
//            setUpdateTime.invoke(entity,now);
//            setUpdateUser.invoke(entity,currentId);
//        }
//    }
//
//}

package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点：拦截所有标记了 @AutoFill 注解的 mapper 方法
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    /**
     * 前置通知：在 mapper 方法执行前自动填充公共字段
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        // 1. 获取数据库操作类型 (INSERT / UPDATE)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 2. 获取被拦截方法的第一个参数（通常是实体对象）
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("被拦截方法无参数，跳过自动填充");
            return;
        }
        Object entity = args[0];

        // 3. 准备要填充的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 4. 根据操作类型通过反射赋值
        try {
            if (operationType == OperationType.INSERT) {
                // INSERT: 填充 createTime, createUser, updateTime, updateUser
                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod("setCreateUser", Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);

            } else if (operationType == OperationType.UPDATE) {
                // UPDATE: 填充 updateTime, updateUser
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            }

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("找不到对应的 setter 方法，请检查实体类是否包含 createTime/createUser/updateTime/updateUser 字段", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法访问 setter 方法，请确保方法是 public 的", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("调用 setter 方法时发生异常", e.getTargetException());
        }
    }
}
