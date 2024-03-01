package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
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

    final CourseClient courseClient; //远程调用课程服务

    final LearningRecordDelayTaskHandler delayTaskHandler; //延迟队列


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

        //3. 结合 lessonId 查询学习记录
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


    /**
     * 更新提交学习记录
     */
    @Override
    public void addLearningRecord(LearningRecordFormDTO recordFormDTO) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.处理学习记录
        boolean isFinished = false; //代表本小节是否已经学完
        if(recordFormDTO.getSectionType().equals(SectionType.EXAM.getValue())){
            //2.1 提交【考试】记录
            isFinished = handleExamRecord(userId,recordFormDTO);

        }else {
            //2.2 提交【视频】播放记录
            isFinished = handleVedioRecord(userId,recordFormDTO);

        }

        //3.处理课表数据
        handleLessonData(recordFormDTO,isFinished);
    }



    /**
     * 处理课表记录
     */
    private void handleLessonData(LearningRecordFormDTO recordFormDTO, boolean isFinished) {

        //1.获取当前用户的ID
        Long userId = UserContext.getUser();
        //1.1 获取当前的课程信息
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getId, recordFormDTO.getLessonId())
                .one();

        if(isFinished) {
            //2. 已经学完，对应的小节数加一
            CourseFullInfoDTO infoDTO = courseClient
                    .getCourseInfoById(recordFormDTO.getLessonId(), false, false);
            if (infoDTO==null){
                throw new BizIllegalException("当前课程数不存在!");
            }
            LambdaUpdateChainWrapper<LearningLesson> updateChainWrapper = lessonService.lambdaUpdate()
                    .set(LearningLesson::getLearnedSections, learningLesson.getLearnedSections() + 1)
                    .set(LearningLesson::getLatestSectionId, recordFormDTO.getSectionId())
                    .set(LearningLesson::getLatestLearnTime, recordFormDTO.getCommitTime())
                    //2.1 判断是否学完全部的小节，若是，则进行更新课程状态
                    .set(infoDTO.getSectionNum()<=learningLesson.getLearnedSections()+1,LearningLesson::getStatus,LessonStatus.FINISHED)
                    .eq(LearningLesson::getUserId, userId)
                    .eq(LearningLesson::getId, recordFormDTO.getLessonId());

            boolean update = lessonService.update(updateChainWrapper);
            if(!update){
                throw new DbException("新增小节数失败!");
            }
        }else {
            //3.若不是第一次学完，则进行更新课表
            boolean update = lessonService.lambdaUpdate()
                    .set(LearningLesson::getLatestSectionId, recordFormDTO.getSectionId())
                    .set(LearningLesson::getLatestLearnTime, recordFormDTO.getCommitTime())
                    //若原来的课表状态为未学习，则更新为学习中
                    .set(learningLesson.getStatus()==LessonStatus.NOT_BEGIN,LearningLesson::getStatus,LessonStatus.LEARNING)
                    .eq(LearningLesson::getId,learningLesson.getId())
                    .update();
            if(!update){
                throw new DbException("更新课表失败!");
            }
        }
    }



    /**
     *  处理视频播放记录
     */
    private boolean handleVedioRecord(Long userId, LearningRecordFormDTO recordFormDTO) {

        //1.查询旧的学习记录
        LearningRecord learningRecord = queryOldRecord(recordFormDTO.getLessonId(),recordFormDTO.getSectionId());

//        LearningRecord learningRecord = learningRecordService.lambdaQuery()
//                .eq(LearningRecord::getUserId, userId)
//                .eq(LearningRecord::getLessonId, recordFormDTO.getLessonId())
//                .eq(LearningRecord::getSectionId,recordFormDTO.getSectionId())
//                .one();

        //1.1判断该学习记录是否存在
        if(learningRecord==null){
            //2. 如果不存在则新增学习记录
            LearningRecord record = BeanUtils.copyProperties(recordFormDTO, LearningRecord.class)
                    .setUserId(userId);
            boolean res = learningRecordService.save(record);
            if(!res){
                throw new DbException("新增学习记录失败!");
            }
            return false; //未完成
        }
        //2.1 若存在记录则进行更新
        //2.2判断当前是否为【第一次】学完本次课程（要求：旧状态为未完成，且本次播放量超过50%）
        boolean isFinished = !learningRecord.getFinished() && recordFormDTO.getDuration() / recordFormDTO.getMoment() < 2;

        //2.3不是第一次学完
        if(!isFinished){

            //3.先将课程信息缓存到 redis 中
            LearningRecord record = new LearningRecord()
                    .setId(learningRecord.getId())
                    .setSectionId(recordFormDTO.getSectionId())
                    .setLessonId(recordFormDTO.getLessonId())
                    .setMoment(recordFormDTO.getMoment()) //【着重处理点】
                    .setFinished(false);
            delayTaskHandler.addLearningRecordTask(record); //内部已经提交延迟任务到延迟队列 DelayQueue
            return false;
        }

        LambdaUpdateChainWrapper<LearningRecord> set = learningRecordService.lambdaUpdate()
                .set(LearningRecord::getMoment, recordFormDTO.getMoment())
                .set(LearningRecord::getFinished, true)
                //若为第一次学完，则进行更新该课程的学习完成记录的时间
                .set(LearningRecord::getFinishTime, recordFormDTO.getCommitTime())
                .eq(LearningRecord::getId,learningRecord.getId());
        boolean update = learningRecordService.update(set);
        if(!update){
            throw new DbException("学习记录更新失败!");
        }

        //5.为了保持缓存与数据库【数据的一致性】，清除 redis 中缓存，之后让 DB 重新将记录加入 redis 中
        delayTaskHandler.cleanRecordCache(learningRecord.getLessonId(),learningRecord.getSectionId());

        return true;
    }


    /**
     * 从缓存中查询对应的学习记录（之前的旧记录）
     */
    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {

        //1.先进行查询缓存
        LearningRecord learningRecord = delayTaskHandler.readRecordCache(lessonId, sectionId);

        //1.1若命中，则直接返回
        if(learningRecord!=null){
            return learningRecord;
        }

        //2.若缓存没有，则查询 DB
        learningRecord = learningRecordService.lambdaQuery()
        .eq(LearningRecord::getUserId, UserContext.getUser())
        .eq(LearningRecord::getLessonId, lessonId)
        .eq(LearningRecord::getSectionId,sectionId)
        .one();

        //2.1 若 DB 中存在，则放入缓存中
        if(learningRecord!=null){
            delayTaskHandler.writeRecordCache(learningRecord);
        }else {
            //2.2 若都不存在，则返回 null
            return null;
        }

        return learningRecord;
    }


    /**
     * 处理考试记录
     */
    private boolean handleExamRecord(Long userId, LearningRecordFormDTO recordFormDTO) {

        //1.封装 po 对象
        LearningRecord learningRecord = BeanUtils.copyProperties(recordFormDTO, LearningRecord.class)
                .setUserId(userId)
                .setFinished(true) //考试完成，代表该课程学习也完成
                .setFinishTime(recordFormDTO.getCommitTime());

        //2.更新保存学习记录
        boolean res = learningRecordService.save(learningRecord);
        if(!res){
            throw new DbException("新增考试记录失败!");
        }

        return true;
    }


}
