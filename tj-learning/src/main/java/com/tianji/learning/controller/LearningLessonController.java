package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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


    @ApiOperation("检查当前课程用户是否可以学习")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId")Long courseId){

        return lessonService.isLessonValid(courseId);
    }


    @ApiOperation("查询用户课表中指定课程状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonByCourseId(@PathVariable("courseId")Long courseId){

        return lessonService.queryLessonByCourseId(courseId);
    }


    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createLessonPlans(@RequestBody @Validated LearningPlanDTO learningPlanDTO){

        lessonService.createLessonPlans(learningPlanDTO);
    }


    @ApiOperation("查询学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyLessonPlans(PageQuery pageQuery){

        return lessonService.queryMyLessonPlans(pageQuery);
    }


    @ApiOperation("统计课程学习人数")
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){

        return lessonService.countLearningLessonByCourse(courseId);
    }


}
