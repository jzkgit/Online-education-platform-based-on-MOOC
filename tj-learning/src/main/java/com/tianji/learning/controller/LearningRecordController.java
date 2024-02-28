package com.tianji.learning.controller;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习记录的相关接口")
@RequiredArgsConstructor
public class LearningRecordController {


    final ILearningRecordService learningRecordService;

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @ApiOperation("查询当前用户指定课程的学习进度")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){

        return  learningRecordService.queryLearningRecordByCourse(courseId);
    }


    /**
     * 更新提交学习记录
     */
    @ApiOperation("更新提交学习记录")
    @PostMapping
    public void addLearningRecord(@RequestBody @Validated LearningRecordFormDTO learningRecordFormDTO){

        learningRecordService.addLearningRecord(learningRecordFormDTO);
    }

}
