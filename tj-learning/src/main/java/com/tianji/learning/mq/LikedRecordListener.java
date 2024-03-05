package com.tianji.learning.mq;


import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
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

/**
 * 点赞业务，MQ 监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikedRecordListener {


    final InteractionReplyServiceImpl replyService;

    /**
     * QA 问答系统 ，消费者
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.liked.times.queue",durable = "true"),
    exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,type = ExchangeTypes.TOPIC),
    key = MqConstants.Key.QA_LIKED_TIMES_KEY))
    public void onMsg(LikedTimesDTO likedTimesDTO){

        log.info("监听到消息：{}",likedTimesDTO);
        InteractionReply reply = replyService.getById(likedTimesDTO.getBizId());
        if(reply==null){
            return;
        }
        reply.setLikedTimes(likedTimesDTO.getLikedTimes()); //更新当前点赞数量
        replyService.updateById(reply); //更新数据库
    }

}
