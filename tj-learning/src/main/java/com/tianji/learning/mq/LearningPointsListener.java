package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.DbException;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.impl.PointsRecordServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;

/**
 * 监听用户积分，获取对应用户积分信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningPointsListener {


    final IPointsRecordService pointsRecordService;


    /**
     * 签到积分
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "sign.points.queue",durable = "true"),
    exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
    key = MqConstants.Key.SIGN_IN))
    public void onMsgBySign(SignInMessage signInMessage){

        log.debug("签到消息被接收：{}",signInMessage.toString());

        //1.进行判断接收的消息是否合法
        if(signInMessage.getPoints()!=null&&signInMessage.getUserId()!=null){
            return;
        }

        //2.进行对应用户的积分保存
        pointsRecordService.addUserPoints(signInMessage, PointsRecordType.SIGN);
    }


    /**
     * 问答积分
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.points.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN))
    public void onMsgByReply(SignInMessage signInMessage){

        log.debug("问答消息被接收：{}",signInMessage.toString());

        //1.进行判断接收的消息是否合法
        if(signInMessage.getPoints()!=null&&signInMessage.getUserId()!=null){
            return;
        }

        //2.进行对应用户的积分保存
        pointsRecordService.addUserPoints(signInMessage, PointsRecordType.QA);

    }



}
