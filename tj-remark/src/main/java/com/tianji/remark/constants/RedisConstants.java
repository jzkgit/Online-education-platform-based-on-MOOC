package com.tianji.remark.constants;

/**
 * redis 数据结构类型常量类
 */
public interface RedisConstants {

    //后面拼接的是对应点赞的业务ID【键】
    String LIKES_BIZ_KEY_PREFIX = "likes:set:biz:";

    //后面拼接的是业务的类型【键】
    String LIKES_TIMES_KEY_PREFIX = "likes:times:type:";

}
