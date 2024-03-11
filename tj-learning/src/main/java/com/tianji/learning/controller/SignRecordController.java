package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 签到控制器
 */
@Api(value = "签到接口")
@Controller
@RequestMapping(value = "/sign-records")
@RequiredArgsConstructor
public class SignRecordController {


    final ISignRecordService signRecordService;

    @ApiOperation("签到服务")
    @PostMapping
    public SignResultVO addSignRecords(){

        return signRecordService. addSignRecords();
    }


    @ApiOperation("查询当前用户的签到记录")
    @GetMapping
    public List<Long> querySignRecords(){

        return signRecordService.querySignRecords();
    }



}
