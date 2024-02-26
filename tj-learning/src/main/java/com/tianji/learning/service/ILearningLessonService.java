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
     * 课表分页查询
     * @param pageQuery
     */
    PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery);

}
