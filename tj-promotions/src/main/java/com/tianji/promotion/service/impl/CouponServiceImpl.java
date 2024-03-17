package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.common.domain.dto.PageDTO;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;

import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tianji.promotion.enums.CouponStatus.*;

/**
 * 优惠券的规则信息 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {


    final CouponServiceImpl couponService;

    final CouponScopeServiceImpl scopeService;

    final CategoryCache categoryCache;

    final IExchangeCodeService codeService; //兑换码服务

    final IUserCouponService userCouponService;

    final StringRedisTemplate redisTemplate;


    /**
     * 新增优惠券功能
     */
    @Override
    @Transactional  //由于当前业务存在多个DB操作，开启事务
    public void saveCoupon(CouponFormDTO couponFormDTO) {

        //1.封装 po，保存当前优惠券信息
        Coupon coupon = BeanUtils.copyBean(couponFormDTO, Coupon.class);
        couponService.save(coupon);

        //2.判断是否限制了使用范围
        if(!couponFormDTO.getSpecific()){
            return;
        }

        //3.若限定了范围，则进行校验
        List<Long> scopes = couponFormDTO.getScopes();
        if(CollUtils.isEmpty(scopes)){
            throw new BadRequestException("分类ID不能为空");
        }

        //4.保存优惠券的限定范围
        List<CouponScope> couponScopes = scopes.stream()
                .map(new Function<Long, CouponScope>() {
                    @Override
                    public CouponScope apply(Long aLong) {

                        return new CouponScope().setBizId(aLong).setType(1).setCouponId(coupon.getId());
                    }
                })
                .collect(Collectors.toList());

        scopeService.saveBatch(couponScopes);
    }



    /**
     * 分页查询优惠券页表
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery couponQuery) {

        //1.根据传入的分页条件，查询po对象
        Page<Coupon> couponPage = couponService.lambdaQuery()
                .eq(couponQuery.getType() != null, Coupon::getDiscountType, couponQuery.getType())
                .eq(couponQuery.getStatus() != null, Coupon::getStatus, couponQuery.getStatus())
                .like(couponQuery.getName() != null, Coupon::getName, couponQuery.getName())
//                .page(couponQuery.toMpPage("",false));
                .page(couponQuery.toMpPageDefaultSortByCreateTimeDesc());//根据创建时间做倒序

        List<Coupon> records = couponPage.getRecords();
        if(records==null){
            return PageDTO.empty(couponPage);
        }

        //2.封装 vo 返回
        List<CouponPageVO> couponPageVOS = BeanUtils.copyList(records, CouponPageVO.class);

        return PageDTO.of(couponPage,couponPageVOS);
    }


    /**
     * 根据ID查询优惠券信息
     */
    @Override
    public CouponDetailVO queryCouponById(Long id) {

        //1.根据 ID 查询对应的信息
        Coupon coupon = couponService.lambdaQuery()
                .eq(Coupon::getId, id)
                .one();

        if(coupon==null){
            return null;
        }

        //2.封装 vo 对象
        CouponDetailVO couponDetailVO = BeanUtils.copyProperties(coupon, CouponDetailVO.class);

        //3.查询对应的 优惠券适用范围 scope
        List<CouponScope> scopes = scopeService.lambdaQuery()
                .eq(CouponScope::getCouponId, id)
                .list();

        //3.1 判断当前优惠券是否存在使用范围，没有则直接返回
        if(scopes==null){
            return couponDetailVO;
        }

        //3.2进行遍历，给 VO 类中使用范围类型赋值
        ArrayList<CouponScopeVO> couponScopeVOS = new ArrayList<>();
        for (CouponScope scope:scopes){
            String name = categoryCache.getNameByLv3Id(scope.getBizId()); //获取到范围名称
            CouponScopeVO couponScopeVO = new CouponScopeVO();
            couponScopeVO.setId(scope.getBizId());
            couponScopeVO.setName(name);
            couponScopeVOS.add(couponScopeVO);
        }
        couponDetailVO.setScopes(couponScopeVOS);

        return couponDetailVO;
    }


    /**
     * 修改优惠券信息
     */
    @Override
    public void updateCouponById(CouponFormDTO couponFormDTO, Long id) {

        //1.查询当前优惠券的状态，是否为待发放状态
        Coupon coupon = couponService.lambdaQuery()
                .eq(Coupon::getId,id)
                .eq(Coupon::getStatus, DRAFT)
                .one();

        //2.若当前状态不是待发放状态，则进行提示
        if(coupon==null){
            throw new BizIllegalException("当前优惠券不是待发放状态，修改失败!");
        }

        //3.反之，进行修改
        BeanUtils.copyProperties(couponFormDTO,coupon);

        couponService.save(coupon);
    }


    /**
     * 删除对应的优惠券
     */
    @Override
    public void deleteCouponById(Long id) {

        //1.查询当前优惠券是否存在
        Coupon coupon = couponService.getById(id);
        if(coupon==null||coupon.getStatus()!= DRAFT){
            throw new BadRequestException("当前优惠券不存在或者在使用中!");
        }

        //2.删除优惠券
        boolean remove = couponService.remove(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getId, id)
                .eq(Coupon::getStatus, DRAFT));

        if(!remove){
            throw new BadRequestException("当前优惠券不存在或者在使用中!");
        }

        //3.将该优惠券中的使用范围删除
        if(!coupon.getSpecific()){
            return;
        }
        scopeService.remove(Wrappers.<CouponScope>lambdaQuery()
                .eq(CouponScope::getCouponId,id));
    }


    /**
     * 发放优惠券
     */
    @Override
    public void issueCoupons(Long couponId, CouponIssueFormDTO couponIssueFormDTO) {

        log.debug("生成优惠券  线程池名称：{}",Thread.currentThread().getName());

        //0.校验参数
        if(couponId==null||!couponId.equals(couponIssueFormDTO.getId())){
            throw new BadRequestException("非法参数!");
        }

        //1.校验当前优惠券的状态，只有待发放和暂停状态才能进行发放优惠券
        Coupon coupon = couponService.getById(couponId);
        if(coupon==null||!coupon.getStatus().equals(DRAFT)||!coupon.getStatus().equals(PAUSE)){
            throw new BadRequestException("当前优惠券不存在或者在使用中!");
        }

        //2.判断当前优惠券是否为立刻发放（true：是  false: 否）
        LocalDateTime now = LocalDateTime.now();
        boolean whetherIssue = coupon.getIssueBeginTime()==null || !coupon.getIssueBeginTime().isAfter(now);

        //3.进行判断是否为立刻发放，进行更新赋值时间状态
        if(whetherIssue){
            //3.1 是立刻发放
            coupon.setIssueBeginTime(couponIssueFormDTO.getIssueBeginTime()==null?now:couponIssueFormDTO.getIssueBeginTime());
            coupon.setIssueEndTime(couponIssueFormDTO.getIssueEndTime());
            coupon.setStatus(ISSUING);  //发放中
        }else {
            //3.2 不是立刻发放
            coupon.setIssueBeginTime(couponIssueFormDTO.getIssueBeginTime());
            coupon.setIssueEndTime(couponIssueFormDTO.getIssueEndTime());
            coupon.setStatus(UN_ISSUE);
        }
        coupon.setTermBeginTime(couponIssueFormDTO.getTermBeginTime());
        coupon.setTermEndTime(couponIssueFormDTO.getTermEndTime());
        coupon.setTermDays(couponIssueFormDTO.getTermDays());

        couponService.updateById(coupon);


        /*
             将需要立刻发放的优惠券信息【在 redis 中初始化符合条件的优惠券信息】，加快响应速率
         */
        //4.若当前优惠券是【立刻发放】，则将其数据存入 redis 的 hash 数据类型中【传值类型：1.优惠券ID 2.领卷开始结束时间 3.发行总数量 4.限领数量】
        if(whetherIssue){

            String couponKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX+couponId;
            redisTemplate.opsForHash().put(couponKey,"issue_begin_time",
                    String.valueOf(DateUtils.toEpochMilli(couponIssueFormDTO.getIssueBeginTime())));
            redisTemplate.opsForHash().put(couponKey,"issue_end_time",
                    String.valueOf(DateUtils.toEpochMilli(couponIssueFormDTO.getIssueEndTime())));
            redisTemplate.opsForHash().put(couponKey,"total_num",String.valueOf(coupon.getTotalNum()));
            redisTemplate.opsForHash().put(couponKey,"user_limit",String.valueOf(coupon.getUserLimit()));
        }



        //5.若发放优惠券时，选择的是指定发放，且之前的状态是待发放，则需要生成兑换码
        if(coupon.getObtainWay().equals(ObtainType.ISSUE)&&coupon.getStatus().equals(DRAFT)){

            codeService.getExchangeCodeInfo(coupon);  //【异步】生成兑换码
        }
    }



    /**
     * 查询发放中的优惠券——用户端
     */
    @Override
    public List<CouponVO> queryIssuingCoupons() {

        //0.获取当前登录用户ID
        Long userId = UserContext.getUser();

        //1.查询 DB 中发放中且为手动领取类型的优惠券
        List<Coupon> couponList = couponService.lambdaQuery()
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .eq(Coupon::getStatus, ISSUING)
                .list();
        if(couponList==null){
            return CollUtils.emptyList();
        }

        //2.查询当前用户的用户优惠券表 userCoupon
        //2.1 查询当前正在发放中的优惠券的ID集合
        Set<Long> couponIds = couponList.stream().map(Coupon::getId).collect(Collectors.toSet());
        //2.2 当前用户符合条件的优惠券的集合
        List<UserCoupon> userCouponList = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .in(UserCoupon::getCouponId, couponIds)
                .list();

        //2.3 使用 map 集合来【表示当前用户下，每一张优惠券所对应的数量】   【使用懒加载策略】
        HashMap<Long, Long> hashMap = new HashMap<>();
        for (UserCoupon userCoupon:userCouponList){
            Long couponNums = hashMap.get(userCoupon.getCouponId()); //对应优惠券的数量
            if(couponNums==null){
                hashMap.put(userCoupon.getCouponId(),1L);   //若之前未进行赋值当前用户所拥有的优惠券的数量，则进行初始化
            }else {
                hashMap.put(userCoupon.getCouponId(), (long) (couponNums.intValue() + 1));
            }
        }

        //2.4 【筛选出当前用户是否有已领取，且状态为未使用的优惠券】
        Map<Long, Long> unUsedCouponMap = userCouponList.stream()
                .filter(c -> c.getStatus() == UserCouponStatus.UNUSED)  //未使用
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));


        //3.封装 VO 返回
        ArrayList<CouponVO> couponVOS = new ArrayList<>();
        couponList.forEach(new Consumer<Coupon>() {
                    public void accept(Coupon coupon) {
                        CouponVO couponVO = BeanUtils.copyBean(coupon, CouponVO.class);
                        /*
                            若当前优惠券还有剩余，并且当前用户领取数量未超过上限，将其状态改为"可领取"
                         */
                        if(coupon.getIssueNum()<coupon.getTotalNum()&&hashMap.get(coupon.getId()).intValue()<coupon.getUserLimit()) {
                            couponVO.setAvailable(true);  //是否可以领取
                        }else {
                            couponVO.setAvailable(false);
                        }

                        /*
                            判断当前优惠券是否已经领取，但是未使用的优惠券，将其状态改为"去使用"
                         */
                        couponVO.setReceived(unUsedCouponMap.get(coupon.getId()) != 0);  //是否可使用

                        couponVOS.add(couponVO);
                    }
                });

        return couponVOS;
    }

}
