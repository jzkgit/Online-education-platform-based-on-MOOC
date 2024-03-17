package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tianji.promotion.enums.CouponStatus.ISSUING;


/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 *
 * 使用 【Redis 结合 MQ】 进行完善
 *
 * 这里的 redis 中的coupon数据是从 【发放优惠券业务】 处进行注入
 */
@Service
@RequiredArgsConstructor
public class UserCouponMQRedissonServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {


    final IUserCouponService userCouponService;

    final CouponMapper couponMapper;

    final StringRedisTemplate redisTemplate;

    final RedissonClient redissonClient;

    final IExchangeCodeService codeService;

    final RabbitMqHelper mqHelper;



    /**
     * 领取优惠券
     *
     * 使用 AOP 进行加锁
     */
    @MyLock(name = "lock:coupon:uid:#{userId}")  //将分布式锁前置，保证访问的原子性
    public void receiveCoupon(Long couponId) {

        if(couponId==null){
            throw new BadRequestException("非法参数!");
        }

        //0.获取当前用户的ID
        Long userId = UserContext.getUser();

        //1.校验当前优惠券是否存在
//        Coupon coupon = couponMapper.selectById(couponId);
        Coupon coupon = getCouponInfoByRedis(couponId);
        if(coupon==null){
            throw new BadRequestException("当前优惠券不存在!");
        }

        //2.是否为发放状态
//        if(!coupon.getStatus().equals(ISSUING)){
//            throw new BadRequestException("当前优惠券不为发放状态，领取失败!");
//        }
        //2.1 判断领取优惠券的时间
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("不在领取优惠券的时间范围内，领取失败!");
        }

        //3.当前优惠券是否充足
//        if(coupon.getTotalNum()<=0||coupon.getIssueNum()>coupon.getTotalNum()){
        if(coupon.getTotalNum()<=0){
            throw new BadRequestException("当前优惠券不充足，领取失败!");
        }


        /*
            使用 redis 中的 hash 结构进行改造【将不同用户所拥有同一优惠券的数量关联在一起，判断是否超出对应优惠券的限领数量】
         */
        //5.结合 redis 中的 hash 结构进行判断，当前优惠券是否达到了领取的上限
        String userKey = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
        Long increment = redisTemplate.opsForHash().increment(userKey, userId.toString(), 1); //返回值为加一后的领取数量

        if (increment > coupon.getUserLimit()) {
            redisTemplate.opsForHash().increment(userKey, userId.toString(), -1);  //恢复原来的值
            throw new BadRequestException("你已经达到领取上限，领取失败!");
        }


        /*
            当以上信息校验完后，使用 MQ 进行转发，完成当前优惠券以及用户券的更新
         */
        //6.将信息使用进行封装
        UserCouponDTO userCouponDTO = new UserCouponDTO();
        userCouponDTO.setCouponId(couponId);
        userCouponDTO.setUserId(userId);

        //6.1 使用 MQ 进行转发
        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
                MqConstants.Key.COUPON_RECEIVE,
                userCouponDTO);
    }



    /*
     * 【完善】将 DB 获取优惠券改为使用 redis 进行获取 优惠券 po 对象（这里获取到的 coupon 属性只有 redis 中所提供的）
     */
    private Coupon getCouponInfoByRedis(Long couponId) {

        //1.拼接 key
        String couponKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX  + couponId;

        //2.从 redis 中获取hash中 键 下的所有对应的值
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(couponKey);

        return BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create()); //调用工具类，转换成 po
    }




    /*********************************************
     * 使用 MQ 进行异步保存优惠券、用户券信息【MQ 消息消费处】
     * *
     * *******************************************
     */
    @Transactional  //有默认的优先级->最低
    public void saveCouponAndUserCouponInfo(UserCouponDTO userCouponDTO) {

        //1. 若没有达到领取的上限，则当前优惠券的总数量减一【在 redis 中更新】
        String couponKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + userCouponDTO.getCouponId();

        /*
            注意：若这里使用 【put】 进行 hash 的传入值覆盖，虽然能够成功，但是不具有原子性
         */
        redisTemplate.opsForHash().increment(couponKey,"total_num",-1);

        /*
            当前优惠券领取业务，在多机版下需要保证原子性 【这里使用 乐观锁 保证业务的原子性】
         */
        //1.1 更新优惠券的发放数量
        couponMapper.incrIssueNum(userCouponDTO.getCouponId());

        //2. 【保存用户券信息】
        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
        savaUserCoupon(userCouponDTO.getUserId(), coupon);
    }
/**********************************************************************************************************************/




    /**
     * 保存用户券
     */
    private void savaUserCoupon(Long userId, Coupon coupon) {

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());

        LocalDateTime termBeginTime = coupon.getTermBeginTime(); //优惠券开始时间
        LocalDateTime termEndTime = coupon.getTermEndTime();  //优惠券截至时间
        //1.判断是否选择了优惠券的有效期范围时间
        if(termBeginTime==null&&termEndTime==null){

            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays()); //开始时间加上有效期的天数
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        userCouponService.save(userCoupon);
    }


    /**
     * 兑换码兑换优惠券
     */
    @Override
    public void exchangeCoupon(String code) {

        //0.校验参数，获取当前用户ID
        if(code==null){
            throw new BadRequestException("无效参数!");
        }
        Long userId = UserContext.getUser();

        //1.解析兑换码
        long parseCode = CodeUtil.parseCode(code);

        //2.判断当前兑换码是否已经兑换过【这里使用redis中的bitMap数据类型】
        boolean res;
        res = codeService.updateExchangeCodeMark(parseCode,true);
        if(res){
            throw new BizIllegalException("当前兑换码已经被使用!");
        }

        try {
            //3.查询当前兑换码是否存在
            ExchangeCode exchangeCode = codeService.lambdaQuery()
                    .eq(ExchangeCode::getCode, code)
                    .eq(ExchangeCode::getUserId, userId)
                    .one();
            if(exchangeCode==null){
                throw new BadRequestException("当前兑换码不存在!");
            }

            //4.判断是否过期
            LocalDateTime now = LocalDateTime.now();
            if(exchangeCode.getExpiredTime().isBefore(now)){
                throw new BadRequestException("当前优惠券已过期!");
            }

            //5.判断兑换码对应的优惠券是否达到限领数量
            //5.1 根据兑换码，获取对应的优惠券
            Long couponId = exchangeCode.getExchangeTargetId();
            Coupon coupon = couponMapper.selectById(couponId);
            if(coupon.getIssueNum()>=coupon.getTotalNum()){
                throw new BadRequestException("已经达到了限领的数量");
            }

            //6.若以上的条件都符合，则优惠券的发放数量加一
            int issueNum = couponMapper.incrIssueNum(couponId);
            if(issueNum<0){
                throw new BadRequestException("优惠券更新数量失败!");
            }

            //7.保存对应用户的优惠券
            savaUserCoupon(userId,coupon);

            //8.更新当前兑换码的状态
            codeService.lambdaUpdate().set(ExchangeCode::getType,ExchangeCodeStatus.USED)
                    .set(ExchangeCode::getUserId,userId)
                    .eq(ExchangeCode::getExchangeTargetId,couponId)
                    .eq(ExchangeCode::getId,parseCode).update();

        }catch (Exception exception){
            //10.将兑换码的状态进行重置
            res = codeService.updateExchangeCodeMark(parseCode,false);
            if(res){
                log.debug("重置兑换码成功!");
            }
            throw exception;
        }

    }


}