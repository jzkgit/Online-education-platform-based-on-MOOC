package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tianji.promotion.enums.CouponStatus.ISSUING;


/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {


    final IUserCouponService userCouponService;

    final CouponMapper couponMapper;

    final StringRedisTemplate redisTemplate;

    final IExchangeCodeService codeService;


    /**
     * 领取优惠券
     */
    @Override
    @Transactional  //当存在一个以上的 CRUD 时，进行开启事务控制
    public void receiveCoupon(Long id) {

        if(id==null){
            throw new BadRequestException("非法参数!");
        }

        //0.获取当前用户的ID
        Long userId = UserContext.getUser();

        //1.校验当前优惠券是否存在
        Coupon coupon = couponMapper.selectById(id);
        if(coupon==null){
            throw new BadRequestException("当前优惠券不存在!");
        }

        //2.是否为发放状态
        if(!coupon.getStatus().equals(ISSUING)){
            throw new BadRequestException("当前优惠券不为发放状态，领取失败!");
        }
        //2.1 判断领取优惠券的时间
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("不在领取优惠券的时间范围内，领取失败!");
        }

        //3.当前优惠券是否充足
        if(coupon.getTotalNum()<=0||coupon.getIssueNum()>coupon.getTotalNum()){
            throw new BadRequestException("当前优惠券不充足，领取失败!");
        }

        //4.是否超出每人限领数量
        //4.1 查询当前用户是否已经领取当前优惠券，若是，领取了几张
        List<UserCoupon> couponList = userCouponService.lambdaQuery()
                .eq(UserCoupon::getCouponId, id)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .list();
        if(couponList!=null){
            long count = couponList.size();
            if(count>=coupon.getUserLimit()){
                throw new BadRequestException("你已经达到领取上限，领取失败!");
            }
        }


        //5.【保存优惠券信息】 若以上流程都正常运行，则将当前优惠券发放数量加一
//        coupon.setIssueNum(coupon.getIssueNum()+1);
//        couponMapper.updateById(coupon);
        /*
            当前优惠券领取业务，再联机版下需要保证原子性 【这里使用 乐观锁 保证业务的原子性】
         */
        couponMapper.incrIssueNum(id);


        //5.1 【保存用户券】
        savaUserCoupon(userId,coupon);

    }



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