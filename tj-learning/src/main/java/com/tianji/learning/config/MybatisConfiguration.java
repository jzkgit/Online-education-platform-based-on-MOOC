package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 *  MyBatis-Plus 动态表名拦截器
 */
@Configuration
public class MybatisConfiguration {


    /**
     * 【动态生成表名】，当进行 【CRUD】 时，会将原来 points_board 表名，动态替换成 TableInfoContext.getInfo() 当前线程获取到的表名 points_board_XX
     */
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {

        Map<String, TableNameHandler> map = new HashMap<>(1);

        //进行判断是否添加了新表名，若没有添加，则默认为原来的表名
        map.put("points_board", (sql, tableName) -> TableInfoContext.getInfo()==null?"points_board":TableInfoContext.getInfo());

        return new DynamicTableNameInnerInterceptor(map);
    }

}
