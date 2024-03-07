package com.tianji.api.config;

import com.tianji.api.client.learning.fallback.LearningClientFallback;
import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import com.tianji.api.client.trade.fallback.TradeClientFallback;
import com.tianji.api.client.user.fallback.UserClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注入 feign 方法调用时的【降级服务类】，给 spring 管理
 */
@Configuration
public class FallbackConfig {

    @Bean
    public LearningClientFallback learningClientFallback(){
        return new LearningClientFallback();
    }

    @Bean
    public TradeClientFallback tradeClientFallback(){
        return new TradeClientFallback();
    }

    @Bean
    public UserClientFallback userClientFallback(){
        return new UserClientFallback();
    }

    @Bean
    public RemarkClientFallback remarkClientFallback(){
        return new RemarkClientFallback();
    }

}
