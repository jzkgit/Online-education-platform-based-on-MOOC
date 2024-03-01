package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 互动提问的问题表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {


    final InteractionQuestionServiceImpl questionService;

    final InteractionQuestionMapper questionMapper;


    /**
     * 新增互动问题服务
     * @param formDTO
     */
    @Override
    public void savaQuestions(QuestionFormDTO formDTO) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.封装 po 对象
        InteractionQuestion interactionQuestion = BeanUtils.copyProperties(formDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(userId)
                .setStatus(QuestionStatus.UN_CHECK)
                .setCreateTime(LocalDateTime.now());

        //3.保存对象
        boolean save = questionService.save(interactionQuestion);
        if(!save){
            throw new DbException("新增互动问题失败!");
        }
    }


    /**
     * 修改互动问题服务
     * @param id
     */
    @Override
    public void updateQuestionsById(QuestionFormDTO formDTO,Long id) {

        //1.获取当前用户 ID
        Long userId = UserContext.getUser();

        //1.1进行校验
        if (StringUtils.isBlank(formDTO.getTitle())||StringUtils.isBlank(formDTO.getDescription())||formDTO.getAnonymity()==null){
            throw new BadRequestException("非法参数!");
        }

        //2.根据ID获取需要修改的问题内容
        LambdaQueryChainWrapper<InteractionQuestion> wrapper = questionService.lambdaQuery()
                .eq(InteractionQuestion::getUserId, userId)
                .eq(InteractionQuestion::getId,id);
        InteractionQuestion interactionQuestion = questionMapper.selectOne(wrapper);
        if(interactionQuestion==null){
            throw new DbException("获取问题内容失败!");
        }

        //3.封装 po
        BeanUtils.copyProperties(formDTO,interactionQuestion);

        //4.修改问题内容
        boolean update = questionService.updateById(interactionQuestion);
        if(!update){
            throw new DbException("修改问题内容失败!");
        }

    }


}
