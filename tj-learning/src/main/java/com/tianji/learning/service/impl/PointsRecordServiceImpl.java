package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 学习积分记录，每个月底清零 服务实现类
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {


    final PointsRecordServiceImpl recordService;


    final StringRedisTemplate redisTemplate;


    /**
     * 保存对应用户的积分信息
     */
    @Override
    public void addUserPoints(SignInMessage signInMessage,PointsRecordType recordType) {

        //0.校验传入的参数
        if(signInMessage==null||recordType==null){
            return; //若抛出异常，会触发 mq 的重试机制【不断的进行消息的发送，直到正确，这里不建议这种做法】
        }

        //1.判断当前类型的积分是否有上限
        int maxPoints = recordType.getMaxPoints(); //当前签到类型签到的积分上限
        int totalPoints = 0;
        int todayPoints = signInMessage.getPoints(); //当天所获得该类型的积分
        if(maxPoints>0){
            //1.1 若有，则判断当前用户是否超出每日上限(查当天)
            LocalDate now = LocalDate.now();
            LocalDateTime beginTime = DateUtils.getWeekBeginTime(now); //当天开始时间
            LocalDateTime endTime = DateUtils.getWeekEndTime(now); //当天结束时间
            QueryWrapper<PointsRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("sum(points) as totalPoints");
            queryWrapper.eq("user_id",signInMessage.getUserId());
            queryWrapper.eq("type",recordType);
            queryWrapper.between("create_time",beginTime,endTime);  //表示当天

            Map<String, Object> recordMap = recordService.getMap(queryWrapper);
            if(recordMap!=null) {
                 totalPoints = Integer.parseInt(recordMap.get("totalPoints").toString());
                //1.2 判断当天已得该类型积分是否超出当前类型的日积分上限
                if (totalPoints >= maxPoints) {
                    return;
                }
                //1.3 若没有超出，则判断当前用户获得的积分与当前积分加起来一共是否超出上限
                if(totalPoints+todayPoints>maxPoints){
                    todayPoints = maxPoints - totalPoints;
                }
            }
        }

        //2.若没有上限，则直接进行保存当前用户该类型下的积分记录
        PointsRecord record = new PointsRecord();
        record.setPoints(todayPoints);
        record.setType(recordType);
        record.setUserId(signInMessage.getUserId());
        recordService.save(record);

        //3.将当前总积分值保存到 【redis】 中
        //3.1 声明 key 值
        LocalDateTime now = LocalDateTime.now();
        String format = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String recordKey = RedisConstants.POINTS_BOARD_KEY_PREFIX  + format;

        //3.2 给对应用户下【累加每次获取不同类型下的总分值】
        redisTemplate.opsForZSet().incrementScore(recordKey,signInMessage.getUserId().toString(),totalPoints);

    }



    /**
     * 查看今日用户积分情况
     */
    @Override
    public List<PointsStatisticsVO> queryTodayPointsInfo() {

        //1.获取当前登录用户ID
        Long userId = UserContext.getUser();

        //2.查询该用户今日积分表信息
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);
        LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);

        QueryWrapper<PointsRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("type,sum(points) as points");  //积分类型、对应的积分值
        queryWrapper.eq("user_id",userId);
        queryWrapper.between("create_time",dayStartTime,dayEndTime);
        queryWrapper.groupBy("type");
        List<PointsRecord> recordList = recordService.list(queryWrapper);
        if(recordList==null){
            return CollUtils.emptyList();
        }

        //3.进行遍历今日积分表，封装 vo 对象
        ArrayList<PointsStatisticsVO> pointsStatisticsVOS = new ArrayList<>();
        for(PointsRecord record:recordList){
            PointsStatisticsVO statisticsVO = new PointsStatisticsVO();
            statisticsVO.setType(record.getType().getDesc());
            statisticsVO.setPoints(record.getPoints());
            statisticsVO.setMaxPoints(record.getType().getMaxPoints()); //单日积分上限

            pointsStatisticsVOS.add(statisticsVO);
        }

        return pointsStatisticsVOS;
    }

}
