package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.msg.SignInMessage;

import java.util.List;

/**
 * 学习积分记录，每个月底清零 服务类
 */
public interface IPointsRecordService extends IService<PointsRecord> {


    /**
     * 保存对应用户的积分信息
     */
    void addUserPoints(SignInMessage signInMessage,PointsRecordType recordType);


    /**
     * 查看今日用户积分情况
     */
    List<PointsStatisticsVO> queryTodayPointsInfo();


}
