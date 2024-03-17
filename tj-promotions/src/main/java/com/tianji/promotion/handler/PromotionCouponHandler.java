package com.tianji.promotion.handler;


import com.tianji.common.constants.MqConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


/**
 * 这里保存优惠券与用户券信息的 MQ 监听处理类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionCouponHandler {


    final IUserCouponService userCouponService;


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "coupon.receive.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.PROMOTION_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_RECEIVE))
    public void saveCouponAndUseCouponInfo(UserCouponDTO msg){

        log.debug("优惠券 MQ 接收到消息：{}",msg);

        userCouponService.saveCouponAndUserCouponInfo(msg);
    }

}
