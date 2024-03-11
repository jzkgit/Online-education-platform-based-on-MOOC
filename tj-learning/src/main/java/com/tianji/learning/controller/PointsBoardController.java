package com.tianji.learning.controller;

import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.service.impl.PointsBoardSeasonServiceImpl;
import com.tianji.learning.service.impl.PointsBoardServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 学霸天梯榜 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
@Api(tags = "积分相关接口")
public class PointsBoardController {


    final PointsBoardSeasonServiceImpl seasonService;


    @ApiOperation("查询赛季列表信息")
    @GetMapping("/seasons/list")
    public List<PointsBoardSeasonVO> querySeasonsInfo(){

        //1.获取当前时间
        LocalDateTime now = LocalDateTime.now();

        //2.查询开始时间小于当前赛季的赛季信息
        List<PointsBoardSeason> seasonList = seasonService.lambdaQuery()
                .le(PointsBoardSeason::getEndTime, now)
                .list();
        if(seasonList==null){
            return CollUtils.emptyList();
        }

        //3.封装 Vo 返回
        return BeanUtils.copyToList(seasonList, PointsBoardSeasonVO.class);
    }






}
