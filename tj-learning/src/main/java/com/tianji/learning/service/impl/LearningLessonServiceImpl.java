package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生课程表 服务实现类
 */
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {


    final CourseClient courseClient; //课程服务 远程调用

    final CatalogueClient catalogueClient; //媒资服务【远程】

    final LearningLessonServiceImpl lessonService; //this

    final LearningLessonMapper learningLessonMapper;

    final LearningRecordMapper learningRecordMapper;

    /**
     * 保存课程到课表
     */
    @Override
    public void addUserLesson(Long userId, List<Long> courseIds) {

        //1.使用 feign 远程调用课程服务，获取课程信息
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);

        //2.将 DTO 封装po实体类
        ArrayList<LearningLesson> learningLessons = new ArrayList<>();

        for (CourseSimpleInfoDTO infoDTO:simpleInfoList){
            LearningLesson learningLesson = new LearningLesson();
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(infoDTO.getId());

            Integer validDuration = infoDTO.getValidDuration(); //课程的有效时间
            if(validDuration!=null){
                LocalDateTime now = LocalDateTime.now();
                learningLesson.setCreateTime(now);
                learningLesson.setExpireTime(now.plusMonths(validDuration));
            }
            learningLessons.add(learningLesson);
        }

        //3.批量添加课程
        lessonService.saveBatch(learningLessons);

    }


    /**
     * 分页查询我的课表
     * @param pageQuery
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery) {


        //1.获取当前登录人
        Long userId = UserContext.getUser();
        if(userId==null){
            throw new BadRequestException("先进行登录!");
        }

        //2.分页查询我的课表
        Page<LearningLesson> page = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(pageQuery.toMpPage("latest_learn_time", false)); //字段、排序规则
        List<LearningLesson> records = page.getRecords(); //获取数 po 数据
        //2.1判断是否当前用户课表是否为空
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }


        //3.使用远程调用，进行属性的封装
        List<Long> courseIds = records.stream()
                .map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        //3.1 进行判断是否为空
        if(CollUtils.isEmpty(simpleInfoList)){
            throw new BizIllegalException("当前课程不存在!");
        }

        //4. 将 DTO集合 封装为 map 类型
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = simpleInfoList.stream()
                //.collect(Collectors.toMap(CourseSimpleInfoDTO::getId, Function.identity())); //值对应的都是对象本身
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c->c));
        //4.1 封装 VO 对象
        ArrayList<LearningLessonVO> learningLessonVOS = new ArrayList<>();
        for(LearningLesson lesson:records){
            //将 po 对应转换为 vo 对象
            LearningLessonVO learningLessonVO = BeanUtils.copyProperties(lesson, LearningLessonVO.class);
            //根据对应的课程 ID 获取对应的课程信息，并继续封装 vo
            CourseSimpleInfoDTO infoDTO = courseSimpleInfoDTOMap.get(lesson.getCourseId());
            if(infoDTO!=null) {
                learningLessonVO.setCourseName(infoDTO.getName());  //课程名称
                learningLessonVO.setCourseCoverUrl(infoDTO.getCoverUrl()); //课程封面
                learningLessonVO.setSections(infoDTO.getSectionNum()); //章节数量
            }

            learningLessonVOS.add(learningLessonVO);
        }

        return PageDTO.of(page,learningLessonVOS);
    }


    /**
     * 查询正在学习的课程信息
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {

        //1.获取当前登录用户的ID
        Long userId = UserContext.getUser();
        if(userId==null){
            throw new BadRequestException("请先进行登录!");
        }

        //2.查询当前用户最近学习的课程信息
        //        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(LearningLesson::getUserId, userId)
//                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
//                .orderByDesc(LearningLesson::getLatestLearnTime)
//                .last("limit 0,1");
//
//        LearningLesson lesson = learningLessonMapper.selectOne(queryWrapper);
        LearningLesson latestLessonInfo = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime) //最近学习课程的时间
                .last("limit 0,1") //取第一条
                .one();

        //2.1 判断是否为空
        if(BeanUtils.isEmpty(latestLessonInfo)){
            return null;
        }

//----------------------------------------------------------------------------------------------------------------------
        //3.远程调用课程服务，给 vo 课程名、章节数、封面赋值
//        LearningLessonVO learningLessonVO = new LearningLessonVO();
//        CourseSearchDTO courseSearchDTO = courseClient.getSearchInfo(latestLessonInfo.getCourseId());
//        if(courseSearchDTO==null){
//            return  null;
//        }
//        learningLessonVO.setCourseName(courseSearchDTO.getName());
//        learningLessonVO.setSections(courseSearchDTO.getSections());
//        learningLessonVO.setCourseCoverUrl(courseSearchDTO.getCoverUrl());
//
        //4.查询总课程数
//        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(LearningLesson::getUserId,userId);
//        Integer lessonCount = learningLessonMapper.selectCount(queryWrapper);
//        learningLessonVO.setCourseAmount(lessonCount);
//----------------------------------------------------------------------------------------------------------------------

        //3.远程调用课程服务，给 vo 课程名、章节数、封面赋值
        CourseFullInfoDTO fullInfoDTO = courseClient
                .getCourseInfoById(latestLessonInfo.getCourseId(), false, false);
        if(fullInfoDTO==null){
            throw new BizIllegalException("课程不存在!");
        }

        //4.查询总课程数
        Integer count = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId).count();

        //5.远程调用课程服务，获取小节名称与小节编号
        Long latestSectionId = latestLessonInfo.getLatestSectionId();
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient
                //由于只有一个课程信息以及ID，而远程调用方法中需要传入集合，所以这里使用 singleList 集合传入单个元素，节省空间使用
                .batchQueryCatalogue(CollUtils.singletonList(latestSectionId));//根据目录id列表查询目录信息
        if(CollUtils.isEmpty(cataSimpleInfoDTOS)){
            throw new BizIllegalException("当前小节不存在!");
        }

        //6.封装 VO 对象返回
        LearningLessonVO learningLessonVO = BeanUtils.copyProperties(latestLessonInfo, LearningLessonVO.class);
        learningLessonVO.setCourseName(fullInfoDTO.getName());
        learningLessonVO.setCourseCoverUrl(fullInfoDTO.getCoverUrl());
        learningLessonVO.setSections(fullInfoDTO.getSectionNum());
        learningLessonVO.setCourseAmount(count);
        CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
        learningLessonVO.setLatestSectionName(cataSimpleInfoDTO.getName());
        learningLessonVO.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());

        return learningLessonVO;
    }


    /**
     * 检查课程是否有效
     * @param courseId
     * @return
     */
    @Override
    public Long isLessonValid(Long courseId) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.检查是否存在当前课程信息
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();

        if(learningLesson==null){
            return null;
        }

        //3.若有，则检查当前课程信息是否为有效状态
        if(learningLesson.getExpireTime()!=null&&learningLesson.getExpireTime().isBefore(LocalDateTime.now())){
            throw new BizIllegalException("当前课程已过期，无法进行观看!");
        }

        return learningLesson.getId();
    }


    /**
     * 查询用户课表中指定课程状态
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.查询当前用户是否有该课程
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(learningLesson==null){
            return null;
        }

        //3.若有，则封装当前课程的状态信息并返回
        LearningLessonVO learningLessonVO = BeanUtils.copyProperties(learningLesson, LearningLessonVO.class);
        return learningLessonVO;
    }


    /**
     * 创建学习计划
     * @param learningPlanDTO
     */
    @Override
    public void createLessonPlans(LearningPlanDTO learningPlanDTO) {

        //1.获取当前用户 ID
        Long userId = UserContext.getUser();

        //2.获取当前课表信息
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getCourseId, learningPlanDTO.getCourseId())
                .eq(LearningLesson::getUserId, userId)
                .one();
        if(learningLesson==null){
            throw new DbException("当前课程还没有加入课表!");
        }

        //3.修改当前课表的信息
        learningLesson.setWeekFreq(learningPlanDTO.getFreq())
                .setPlanStatus(PlanStatus.PLAN_RUNNING);
        boolean update = lessonService.updateById(learningLesson);
        if(!update){
            throw new DbException("修改学习计划失败!");
        }

    }


    /**
     * 查询学习计划
     * @param pageQuery
     */
    @Override
    public LearningPlanPageVO queryMyLessonPlans(PageQuery pageQuery) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.获取当前的课程计划信息
        //2.1 todo 查询积分



        //2.2 查询本周计划的总小节数
        QueryWrapper<LearningLesson> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(week_frep) as plansTotal"); //表示需要查询的数据
        queryWrapper.eq("user_id",userId);
        queryWrapper.eq("plan_status",PlanStatus.PLAN_RUNNING);
        queryWrapper.in("status",LessonStatus.NOT_BEGIN,LessonStatus.LEARNING);

        /*
         * 这里的map以 {plansTotal:8,plansTotal:6} 的形式展现
         */
        Map<String, Object> lessonServiceMap = lessonService.getMap(queryWrapper);
//        if(lessonServiceMap==null){
//            throw new DbException("当前用户没有设定本周的学习计划!");
//        }

        //2.3 以值取键，得到本周所有小节的总频率
        Integer plansTotal = 0;
        if(lessonServiceMap!=null&&lessonServiceMap.get("plansTotal")!=null) {
            plansTotal = Integer.valueOf(lessonServiceMap.get("plansTotal").toString());
        }

        //2.4 查询本周已经完成的计划数量
        LocalDate now = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(now); //本周开始时间
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(now); //本周结束时间
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getUserId,userId)
                .eq(LearningRecord::getFinished,true)
                .between(LearningRecord::getFinishTime,weekBeginTime,weekEndTime);
        Integer finishPlans = learningRecordMapper.selectCount(wrapper);

        //3.查询课表的数据，封装 PageVo 中 list 数据
        Page<LearningLesson> lessonPage = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningLesson> lessonPageRecords = lessonPage.getRecords();
        //3.1 判断是否为空，若为空，则返回空值记录
        if(CollUtils.isEmpty(lessonPageRecords)){
            LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
            learningPlanPageVO.setList(CollUtils.emptyList());  //空值集合
            learningPlanPageVO.setPages(0L);
            learningPlanPageVO.setTotal(0L);
            return learningPlanPageVO;
        }

        //4. 远程调用课程服务，获取课程信息（名称）
        List<Long> courseIds = lessonPageRecords.stream()
                .map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if(simpleInfoList==null){
            throw new BizIllegalException("当前课程不存在!");
        }
        //4.1 将课程信息集合转化为 map 集合，以便于赋值到 list 集合中
        Map<Long, CourseSimpleInfoDTO> simpleInfoDTOMap = simpleInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));


        //5. 查询对应课程的学习记录表，记录"每个课程下" 已经学完的小节数量【本周】
        QueryWrapper<LearningRecord> lambdaQueryWrapper = new QueryWrapper<>();

        /*
            这里，由于 count(*) 中得出来的值没有对应的属性可以赋值，所以找一个临时的属性作代替（这里找的是 userId）
         */
        lambdaQueryWrapper.select("lesson_id as lessonId","count(*) as userId");

        lambdaQueryWrapper.eq("user_id",userId);
        lambdaQueryWrapper.eq("finished",true);
        lambdaQueryWrapper.between("finish_time",weekBeginTime,weekEndTime);
        lambdaQueryWrapper.groupBy("lesson_id");

        List<LearningRecord> learningRecords = learningRecordMapper.selectList(lambdaQueryWrapper);
        //5.1 将 list 集转换为 map 集合，以便于在另一个 list 中进行传值
        Map<Long, LearningRecord> recordMap = learningRecords.stream()
                .collect(Collectors.toMap(LearningRecord::getLessonId, r -> r));


        //封装 VO 返回
        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        learningPlanPageVO.setWeekTotalPlan(plansTotal); //总的计划学习数量
        learningPlanPageVO.setWeekFinished(finishPlans); //本周完成的计划数量
        ArrayList<LearningPlanVO> learningPlanVOS = new ArrayList<>();
        lessonPageRecords.stream()
                .forEach(new Consumer<LearningLesson>() {
                    @Override
                    public void accept(LearningLesson learningLesson) {
                        LearningPlanVO learningPlanVO = new LearningPlanVO();
                        BeanUtils.copyProperties(learningLesson,learningPlanVO);
                        CourseSimpleInfoDTO infoDTO = simpleInfoDTOMap.get(learningPlanVO.getCourseId());
                        if(infoDTO!=null) {
                            learningPlanVO.setCourseName(infoDTO.getName()); //课程名称
                            learningPlanVO.setSections(infoDTO.getSectionNum()); //课程章节数量
                        }

                        LearningRecord learningRecord = recordMap.get(learningPlanVO.getId());
                        if(learningLesson!=null) {
                            learningPlanVO.setWeekLearnedSections(learningRecord.getUserId().intValue()); //本周已学习章节数
                        }else {
                            learningPlanVO.setWeekLearnedSections(0); //默认为 0
                        }
                        learningPlanVOS.add(learningPlanVO);
                    }
                });
        learningPlanPageVO.setList(learningPlanVOS); //记录数据集合
        learningPlanPageVO.setTotal(lessonPage.getTotal()); //记录总条数
        learningPlanPageVO.setPages(lessonPage.getPages()); //记录总页数

        return learningPlanPageVO;
    }


    /**
     * 统计课程学习人数
     * @param courseId
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {

        //1.查询当前课程的信息
        CourseSearchDTO searchInfo = courseClient.getSearchInfo(courseId);
        if(courseId==null){
            throw new DbException("当前课程查询失败!");
        }

        //2.统计人数
        Integer sold = searchInfo.getSold();
        return sold;
    }


}
