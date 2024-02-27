package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 学生课程表 前端控制器
 */
@Api(tags = "我的课表相关")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    final ILearningLessonService lessonService;

    @ApiOperation(value = "分页查询我的课表")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery){

        return lessonService.queryMyLesson(pageQuery);
    }


    @ApiOperation(value = "查询正在学习的课程")
    @GetMapping("/now")
    public LearningLessonVO queryMyCurrentLesson(){

        return lessonService.queryMyCurrentLesson();
    }


}
