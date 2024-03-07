package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * 点赞业务【降级】处理类
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {


    @Override
    public RemarkClient create(Throwable cause) {

        log.error("查询点赞业务异常，进行降级",cause);

        //当前方法出错时的返回默认值
        return new RemarkClient() {
            @Override
            public Set<Long> queryWhetherLiked(List<Long> bizIds) {
                return null;
            }
        };

    }

}
