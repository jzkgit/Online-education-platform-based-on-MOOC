package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
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
import com.tianji.common.utils.*;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
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
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId,c->c));
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


    @Resource
    private LearningLessonMapper learningLessonMapper;

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


}
