package com.tianji.promotion.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;


/**
 * 自定义 AOP 注解
 */
@Retention(RetentionPolicy.RUNTIME) //运行时生效
@Target(ElementType.METHOD)
public @interface MyLock {

    /*
        以下属性均可在该注解内赋值【这里表示分布式锁的属性】
     */

    String name();

    long waitTime() default 1;  //获取锁的等待时间

    long leaseTime() default -1; //锁失效时间

    TimeUnit unit() default TimeUnit.SECONDS; //失效时间单位

    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;

    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;
}
