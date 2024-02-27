package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 学习记录表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {


    final LearningLessonServiceImpl lessonService;

    final LearningRecordServiceImpl learningRecordService;


    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {

        //1.获取用户 ID
        Long userId = UserContext.getUser();

        //2. 结合 courseID 以及 userId ，查询课表信息
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId)
                .one();
        if(learningLesson==null){
            throw new BizIllegalException("该课程未加入课表!");
        }

        //3. 结合 userId、lessonId 查询学习记录
        List<LearningRecord> learningRecords = learningRecordService.lambdaQuery()
                .eq(LearningRecord::getLessonId, learningLesson.getId())
                .list();
        if(learningRecords==null){
            return null;
        }

        //4. 封装 DTO 返回
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        //4.1 进行赋值转换
        List<LearningRecordDTO> learningRecordDTOS = BeanUtils.copyList(learningRecords, LearningRecordDTO.class);
        learningLessonDTO.setRecords(learningRecordDTOS);

        return learningLessonDTO;
    }

}
