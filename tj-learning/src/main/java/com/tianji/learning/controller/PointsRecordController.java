package com.tianji.learning.controller;

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



}
