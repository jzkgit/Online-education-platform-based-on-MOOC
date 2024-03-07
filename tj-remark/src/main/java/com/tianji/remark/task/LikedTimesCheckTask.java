package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import com.tianji.remark.service.impl.LikedRecordRedisServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 点赞的定时任务类
 */
@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {


    final List<String> BIZ_TYPE=List.of("QA","NOTE"); //业务类型
    final int MAX_BIZ_SIZE = 30; //每次获取业务的数量

    final LikedRecordRedisServiceImpl likedRecordRedisService;

    @Scheduled(cron = "0/20 * * * * *") //表示每间隔20s执行一次
    public void checkLikesTimes(){

        for(String type:BIZ_TYPE){ //根据集合信息，进行轮流查询
            likedRecordRedisService.readLikesAndMsg(type,MAX_BIZ_SIZE); //进行批量发送
        }

    }


}
