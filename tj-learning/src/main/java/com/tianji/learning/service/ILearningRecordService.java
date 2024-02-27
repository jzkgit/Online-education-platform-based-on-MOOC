package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.po.LearningRecord;

/**
 * 学习记录表 服务类
 */
public interface ILearningRecordService extends IService<LearningRecord> {


    /**
     * 查询当前用户指定课程的学习进度
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

}
