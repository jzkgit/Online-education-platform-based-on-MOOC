package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATE_TIME;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_LIKED_TIME;

/**
 * 互动问题的回答或评论 服务实现类
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {


    final InteractionReplyServiceImpl replyService;

//    final InteractionQuestionServiceImpl questionService;  //避免相互注入的问题

    final InteractionReplyMapper replyMapper;

    final InteractionQuestionMapper questionMapper;


    /**
     * 隐藏或显示评论————管理端
     * @param id
     * @param hidden
     */
    @Override
    public void whetherHiddenComment(Long id, boolean hidden) {

        //1.获取当前评论信息
        InteractionReply interactionReply = replyService.lambdaQuery()
                .eq(id != null, InteractionReply::getId, id)
                .one();
        if(interactionReply==null){
            return;
        }

        //2.判断当前评论是否为回答信息
        Integer replyTimes = interactionReply.getReplyTimes(); //获取评论数量
        if(replyTimes>0){
            //2.1 若是，则判断当前回答下是否有评论，隐藏对应评论
            LambdaUpdateChainWrapper<InteractionReply> set = replyService.lambdaUpdate()
                    .eq(InteractionReply::getAnswerId, id)
                    .set(InteractionReply::getHidden, hidden);
            boolean update = replyService.update(set);
            if(!update){
                throw new DbException("修改回答状态失败!");
            }
        }else {
            interactionReply.setHidden(hidden);
            int update = replyMapper.updateById(interactionReply);
            if(update<=0){
                throw new DbException("修改评论状态失败!");
            }
        }

    }


    /**
     * 新增评论或回答
     * @param replyDTO
     */
    @Override
    public void addCommentOrReply(ReplyDTO replyDTO) {

        //1.获取当前回答或评论用户ID
        Long userId = UserContext.getUser();

        //1.1 进行新增回答信息
        InteractionReply interactionReply = BeanUtils.copyProperties(replyDTO, InteractionReply.class);
        interactionReply.setUserId(userId);
        boolean save = replyService.save(interactionReply);
        if(!save){
            throw new DbException("新增回答信息失败!");
        }

        //2.根据是否存在上级回答的ID，来判断当前是评论还是回答
        Long answerId = replyDTO.getAnswerId();

        //2.1 查询当前问题信息
        InteractionQuestion interactionQuestion = questionMapper.selectOne(Wrappers.<InteractionQuestion>lambdaQuery()
                .eq(InteractionQuestion::getId, replyDTO.getQuestionId()));

        if(answerId==0L){
            //2.2 是回答，则需要修改最近一次的回答ID，并回答数加一
            interactionQuestion.setLatestAnswerId(interactionReply.getAnswerId());
            interactionQuestion.setAnswerTimes(interactionQuestion.getAnswerTimes()+1);

        }else {
            //2.3 是评论，则累加回答下的评论次数
            InteractionReply reply = replyService.lambdaQuery()
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .one();
            reply.setReplyTimes(reply.getReplyTimes()+1);
            boolean update = replyService.updateById(reply);
            if(!update){
                throw new DbException("当前评论的次数修改失败!");
            }
        }

        //3.判断是否是学生提交的回答或评论
        //3.1 若是，则进行修改问题的查看状态(0-未查看)
        if(replyDTO.getIsStudent()){
            interactionQuestion.setStatus(QuestionStatus.UN_CHECK);
        }
        int update = questionMapper.updateById(interactionQuestion);
        if(update<0){
            throw new DbException("当前回答信息的最近一次回答者修改失败!");
        }

    }


}
