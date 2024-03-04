package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
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
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.enums.QuestionStatus;
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

    final InteractionReplyMapper replyMapper;


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

        //2.判断当前评论是否为回答信息
        Integer replyTimes = interactionReply.getReplyTimes(); //获取评论数量
        if(replyTimes>0){
            //2.1 若是，则判断当前回答下是否有评论，隐藏对应评论
            LambdaUpdateChainWrapper<InteractionReply> set = replyService.lambdaUpdate()
                    .eq(InteractionReply::getAnswerId, interactionReply.getTargetReplyId())
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

}
