package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.impl.PointsRecordServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习积分记录，每个月底清零 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/points")
@Api(tags = "积分相关接口")
public class PointsRecordController {


    final PointsRecordServiceImpl recordService;


    @ApiOperation("查看今日用户积分情况")
    @GetMapping(value = "/today")
    public List<PointsStatisticsVO> queryTodayPointsInfo(){

        return recordService.queryTodayPointsInfo();
    }


}
