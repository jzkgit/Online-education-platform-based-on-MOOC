//package com.tianji.promotion.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.UserContext;
//import com.tianji.promotion.domain.po.Coupon;
//import com.tianji.promotion.domain.po.ExchangeCode;
//import com.tianji.promotion.domain.po.UserCoupon;
//import com.tianji.promotion.enums.ExchangeCodeStatus;
//import com.tianji.promotion.enums.UserCouponStatus;
//import com.tianji.promotion.mapper.CouponMapper;
//import com.tianji.promotion.mapper.UserCouponMapper;
//import com.tianji.promotion.service.IExchangeCodeService;
//import com.tianji.promotion.service.IUserCouponService;
//import com.tianji.promotion.utils.CodeUtil;
//import com.tianji.promotion.utils.RedisLock;
//import lombok.RequiredArgsConstructor;
//import org.redisson.api.RedissonClient;
//import org.springframework.aop.framework.AopContext;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static com.tianji.promotion.enums.CouponStatus.ISSUING;
//
//
///**
// * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
// *
// * 使用 【使用分布式锁】 进行完善
// */
//@Service
//@RequiredArgsConstructor
//public class UserCouponRedisServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
//
//
//    final IUserCouponService userCouponService;
//
//    final CouponMapper couponMapper;
//
//    final StringRedisTemplate redisTemplate;
//
//    final IExchangeCodeService codeService;
//
//
//    /**
//     * 领取优惠券
//     */
//    @Override
////    @Transactional  //当存在一个以上的 CRUD 时，进行开启事务控制
//    public void receiveCoupon(Long id) {
//
//        if(id==null){
//            throw new BadRequestException("非法参数!");
//        }
//
//        //0.获取当前用户的ID
//        Long userId = UserContext.getUser();
//
//        //1.校验当前优惠券是否存在
//        Coupon coupon = couponMapper.selectById(id);
//        if(coupon==null){
//            throw new BadRequestException("当前优惠券不存在!");
//        }
//
//        //2.是否为发放状态
//        if(!coupon.getStatus().equals(ISSUING)){
//            throw new BadRequestException("当前优惠券不为发放状态，领取失败!");
//        }
//        //2.1 判断领取优惠券的时间
//        LocalDateTime now = LocalDateTime.now();
//        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime())){
//            throw new BadRequestException("不在领取优惠券的时间范围内，领取失败!");
//        }
//
//        //3.当前优惠券是否充足
//        if(coupon.getTotalNum()<=0||coupon.getIssueNum()>coupon.getTotalNum()){
//            throw new BadRequestException("当前优惠券不充足，领取失败!");
//        }
//
//
//        /*
//            以下步骤需要实现原子性，存在超卖问题【synchronized 悲观锁只适用于单机版，在多个JVM下锁不共享】
//         */
////        synchronized (userId.toString().intern()) {  //这里使用 .intern，表示当前字符串若之前存在，则从常量池中进行获取【保证地址的一致性】
////
////            //这里获取当前类的代理对象，使用代理对象调用当前事务方法
////            IUserCouponService userCouponProxy = (IUserCouponService) AopContext.currentProxy();
////            userCouponProxy.saveCouponAndUserCouponInfo(id, userId, coupon);
////
//////            saveCouponAndUserCouponInfo(id, userId, coupon);    //这是调用原对象的方法，即非事务方法调用事务方法，事务失效
////        }
//
//
//         /********************************
//            以下步骤需要实现原子性，存在超卖问题 【使用 setNX 实现分布式锁】
//         **********************************/
//
//        String userKey = "lock:coupon:uid" + userId;
//        RedisLock redisLock = new RedisLock(userKey,redisTemplate);  //声明分布式锁
//
//        try {
//            boolean isLock = redisLock.tryLock(20, TimeUnit.SECONDS);
//            if (!isLock) {
//                throw new BadRequestException("业务超时，执行失败!");
//            }
//
//            //这里获取当前类的代理对象，使用代理对象调用当前事务方法【保证事务的有效性，不然会失效】
//            IUserCouponService userCouponProxy = (IUserCouponService) AopContext.currentProxy();
//            userCouponProxy.saveCouponAndUserCouponInfo(id, userId, coupon);
//
//        }finally {
//            //删除锁
//            redisLock.unlock();
//        }
//
//    }
//
//
//
//    /*********************************************
//     * 保存优惠券、用户券信息
//     * *
//     * 【避免超卖问题】，这里使用事务以及乐观锁、悲观锁策略
//     * *
//     * *******************************************
//     */
//    @Transactional
//    public void saveCouponAndUserCouponInfo(Long couponId, Long userId, Coupon coupon) {
//
//        //4.是否超出每人限领数量
//        //4.1 查询当前用户是否已经领取当前优惠券，若是，领取了几张
//        List<UserCoupon> couponList = userCouponService.lambdaQuery()
//                .eq(UserCoupon::getCouponId, couponId)
//                .eq(UserCoupon::getUserId, userId)
//                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
//                .list();
//        if (couponList != null) {
//            long count = couponList.size();
//            if (count >= coupon.getUserLimit()) {
//                throw new BadRequestException("你已经达到领取上限，领取失败!");
//            }
//        }
//
//
//        //5.【保存优惠券信息】 若以上流程都正常运行，则将当前优惠券发放数量加一
////        coupon.setIssueNum(coupon.getIssueNum()+1);
////        couponMapper.updateById(coupon);
//        /*
//            当前优惠券领取业务，再联机版下需要保证原子性 【这里使用 乐观锁 保证业务的原子性】
//         */
//        couponMapper.incrIssueNum(couponId);
//
//        //5.1 【保存用户券信息】
//        savaUserCoupon(userId, coupon);
//    }
///**********************************************************************************************************************/
//
//
//
//
//    /**
//     * 保存用户券
//     */
//    private void savaUserCoupon(Long userId, Coupon coupon) {
//
//        UserCoupon userCoupon = new UserCoupon();
//        userCoupon.setUserId(userId);
//        userCoupon.setCouponId(coupon.getId());
//
//        LocalDateTime termBeginTime = coupon.getTermBeginTime(); //优惠券开始时间
//        LocalDateTime termEndTime = coupon.getTermEndTime();  //优惠券截至时间
//        //1.判断是否选择了优惠券的有效期范围时间
//        if(termBeginTime==null&&termEndTime==null){
//
//            termBeginTime = LocalDateTime.now();
//            termEndTime = termBeginTime.plusDays(coupon.getTermDays()); //开始时间加上有效期的天数
//        }
//        userCoupon.setTermBeginTime(termBeginTime);
//        userCoupon.setTermEndTime(termEndTime);
//        userCouponService.save(userCoupon);
//    }
//
//
//    /**
//     * 兑换码兑换优惠券
//     */
//    @Override
//    public void exchangeCoupon(String code) {
//
//        //0.校验参数，获取当前用户ID
//        if(code==null){
//            throw new BadRequestException("无效参数!");
//        }
//        Long userId = UserContext.getUser();
//
//        //1.解析兑换码
//        long parseCode = CodeUtil.parseCode(code);
//
//        //2.判断当前兑换码是否已经兑换过【这里使用redis中的bitMap数据类型】
//        boolean res;
//        res = codeService.updateExchangeCodeMark(parseCode,true);
//        if(res){
//            throw new BizIllegalException("当前兑换码已经被使用!");
//        }
//
//        try {
//            //3.查询当前兑换码是否存在
//            ExchangeCode exchangeCode = codeService.lambdaQuery()
//                    .eq(ExchangeCode::getCode, code)
//                    .eq(ExchangeCode::getUserId, userId)
//                    .one();
//            if(exchangeCode==null){
//                throw new BadRequestException("当前兑换码不存在!");
//            }
//
//            //4.判断是否过期
//            LocalDateTime now = LocalDateTime.now();
//            if(exchangeCode.getExpiredTime().isBefore(now)){
//                throw new BadRequestException("当前优惠券已过期!");
//            }
//
//            //5.判断兑换码对应的优惠券是否达到限领数量
//            //5.1 根据兑换码，获取对应的优惠券
//            Long couponId = exchangeCode.getExchangeTargetId();
//            Coupon coupon = couponMapper.selectById(couponId);
//            if(coupon.getIssueNum()>=coupon.getTotalNum()){
//                throw new BadRequestException("已经达到了限领的数量");
//            }
//
//            //6.若以上的条件都符合，则优惠券的发放数量加一
//            int issueNum = couponMapper.incrIssueNum(couponId);
//            if(issueNum<0){
//                throw new BadRequestException("优惠券更新数量失败!");
//            }
//
//            //7.保存对应用户的优惠券
//            savaUserCoupon(userId,coupon);
//
//            //8.更新当前兑换码的状态
//            codeService.lambdaUpdate().set(ExchangeCode::getType,ExchangeCodeStatus.USED)
//                    .set(ExchangeCode::getUserId,userId)
//                    .eq(ExchangeCode::getExchangeTargetId,couponId)
//                    .eq(ExchangeCode::getId,parseCode).update();
//
//        }catch (Exception exception){
//            //10.将兑换码的状态进行重置
//            res = codeService.updateExchangeCodeMark(parseCode,false);
//            if(res){
//                log.debug("重置兑换码成功!");
//            }
//            throw exception;
//        }
//
//    }
//
//
//}