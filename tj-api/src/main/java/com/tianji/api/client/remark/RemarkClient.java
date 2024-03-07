package com.tianji.api.client.remark;


import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(contextId = "remark",value = "remark-service",fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {


    /**
     * 查询当前用户是否点赞了指定的业务
     * @param bizIds
     * @return
     */
    @GetMapping("/likes/list")
    public Set<Long> queryWhetherLiked(@RequestParam("bizIds") List<Long> bizIds);


}
