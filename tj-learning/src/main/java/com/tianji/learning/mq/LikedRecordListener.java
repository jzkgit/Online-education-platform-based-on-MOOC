package com.tianji.learning.mq;


import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.DbException;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.impl.InteractionReplyServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 点赞业务，MQ 监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikedRecordListener {


    final InteractionReplyServiceImpl replyService;

    /**
     * QA 问答系统 【消费者】
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.liked.times.queue",durable = "true"),
    exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,type = ExchangeTypes.TOPIC),
    key = MqConstants.Key.QA_LIKED_TIMES_KEY))
    public void onMsg(List<LikedTimesDTO> timesDTOList){

        log.info("监听到消息：{}",timesDTOList);

        //1.进行封装业务集合
        ArrayList<InteractionReply> interactionReplies = new ArrayList<>();
        for(LikedTimesDTO likedTimesDTO:timesDTOList){
            InteractionReply interactionReply = new InteractionReply();
            interactionReply.setId(likedTimesDTO.getBizId());   //业务ID
            interactionReply.setLikedTimes(likedTimesDTO.getLikedTimes()); //总点赞数
            interactionReplies.add(interactionReply);
        }

        //2.批量更新到 DB
        boolean update = replyService.updateBatchById(interactionReplies);
        if(!update){
            throw new DbException("更新点赞数失败!");
        }
    }

}
