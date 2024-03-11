package com.tianji.learning.mq;


import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * MQ 购买课程消息通知
 */
@Slf4j
@Component
@RequiredArgsConstructor //注入对应接口类的构造类
public class LessonChangeListener {

    final ILearningLessonService lessonService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
    exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
    key = MqConstants.Key.ORDER_PAY_KEY))
    public void onMsg(OrderBasicDTO basicDTO){

        log.info("接收了消息，用户{}，添加的课程{}",basicDTO.getUserId(),basicDTO.getCourseIds());

        //1.校验当前传入的课程信息
        if(basicDTO.getUserId()==null|| CollUtils.isEmpty(basicDTO.getCourseIds()) ||basicDTO.getOrderId()==null){
            return; //若传入参数有问题，则直接结束，不要抛出异常，以免触发MQ的重试机制
        }

        //2.保存课程到课表
        lessonService.addUserLesson(basicDTO.getUserId(),basicDTO.getCourseIds());
    }


}
