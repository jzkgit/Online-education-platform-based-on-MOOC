package com.tianji.learning.task;


import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 将不同赛季的排名数据，以【分表】的形式进行保存（使用定时任务）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointBoardPersistentHandler {


    final IPointsBoardSeasonService seasonService;

    final StringRedisTemplate redisTemplate;

    private IPointsBoardService boardService;


    /**
     * 【创建历史榜单表】
     * 创建上个赛季（上个月）的榜单表（在 sql 中），用于保存赛季历史记录
     */
//    @Scheduled(cron = "0 0 3 1 * ?") //每个月一号的凌晨三点进行执行 【单机版】
    @XxlJob(value = "createSeasonTableJob")
    public void createPointBoardTableOfLastSeason(){

        //1.获取上个月当前当前时间点
        LocalDateTime minusMonths = LocalDateTime.now().minusMonths(1); //表示获取当前时间，并将月份减一

        //2.查询赛季表获取赛季信息
        PointsBoardSeason boardSeason = seasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, minusMonths)
                .ge(PointsBoardSeason::getEndTime, minusMonths)
                .one();
        if(boardSeason==null){
            return;
        }

        //3.创建上赛季的榜单表
        seasonService.createPointsBoardLatestTable(boardSeason.getId());
    }


    /**
     * 【持久化数据到创建的历史榜单表中】
     *
     * 这里使用【分片广播】的形式，进行不同赛季表的赋值操作
     */
    @XxlJob(value = "savePointsBoard2DB")
    public void savaPointsBoardToDB(){

        //1.获取上个月的时间点，获取上个月赛季表的信息
        LocalDateTime now = LocalDateTime.now();    //获取当前时间点
        //1.1将当前时间点减去一个月，得到上个赛季时间点
        LocalDateTime lastTime = now.minusMonths(1);
        PointsBoardSeason season = seasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, lastTime)
                .ge(PointsBoardSeason::getEndTime, lastTime)
                .one();

        //2.判断上赛季表是否存在
        if(season==null){
            return;
        }

        //3.计算动态表名，存入 TableInfoContext 线程中，给 mp 拦截器传入数据
        String tableName = LearningConstants.POINTS_BOARD_TABLE_PREFIX + season.getId();
        TableInfoContext.setInfo(tableName); //传入后，之后的【CRUD】都将围绕这张表进行

        //4.从 redis 中获取对应赛季信息
        String format = lastTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String recordKey = RedisConstants.POINTS_BOARD_KEY_PREFIX  + format;


        //4.1 自定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex();  //获取当前分片的下表索引(下标从0开始)
        int shardTotal = XxlJobHelper.getShardTotal();  //获取总分片数


        int pageNo = shardIndex +1;  //由于分片索引下标从0开始，所以这里进行加一
        int pageSize = 1000;  //每页查询的数据条数
        PointsBoardQuery pointsBoardQuery = new PointsBoardQuery();
        pointsBoardQuery.setPageNo(pageNo);
        pointsBoardQuery.setPageSize(pageSize);
        while (true){

            List<PointsBoard> pointsBoards = boardService.queryCurrentBoard(recordKey, pointsBoardQuery);
            //4.2 若分页查询无数据，则直接跳出循环
            if(pointsBoards==null){
                break;
            }

            //5.将信息持久化到对应的表中
            /*
                历史赛季表属性只有：id、user_id、points
             */
            for (PointsBoard pointsBoard : pointsBoards) {
                pointsBoard.setId(Long.valueOf(pointsBoard.getRank())); //将原来的排名属性赋值给分表后的ID属性
                pointsBoard.setRank(null);
            }

            boardService.saveBatch(pointsBoards);
//            pageNo++;
            pageNo = pageNo + shardTotal;   //每一页的间隔为当前分片的总数
        }


        //6.清空 TableInfoContext 线程中的历史表名
        TableInfoContext.remove();

    }

}
