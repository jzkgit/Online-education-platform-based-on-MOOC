package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * 学生课程表 服务类
 */
public interface ILearningLessonService extends IService<LearningLesson> {


    /**
     * 保存课程到课表
     * @param userId
     * @param courseIds
     */
    void addUserLesson(Long userId, List<Long> courseIds);


    /**
     * 分页查询我的课表
     * @param pageQuery
     */
    PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery);


    /**
     * 查询正在学习的课程
     */
    LearningLessonVO queryMyCurrentLesson();


    /**
     * 检查课程是否有效
     * @param courseId
     */
    Long isLessonValid(Long courseId);


    /**
     * 查询用户课表中指定课程状态
     * @param courseId
     */
    LearningLessonVO queryLessonByCourseId(Long courseId);

}
