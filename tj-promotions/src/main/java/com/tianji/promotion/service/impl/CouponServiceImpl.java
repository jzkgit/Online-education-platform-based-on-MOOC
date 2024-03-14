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
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;

import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tianji.promotion.enums.CouponStatus.*;

/**
 * 优惠券的规则信息 服务实现类
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {


    final CouponServiceImpl couponService;

    final CouponScopeServiceImpl scopeService;

    final CategoryCache categoryCache;


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
    public void issueCoupons(Long id, CouponIssueFormDTO couponIssueFormDTO) {

        //0.校验参数
        if(id==null||!id.equals(couponIssueFormDTO.getId())){
            throw new BadRequestException("非法参数!");
        }

        //1.校验当前优惠券的状态
        Coupon coupon = couponService.getById(id);
        if(coupon==null||!coupon.getStatus().equals(DRAFT)||!coupon.getStatus().equals(UN_ISSUE)){
            throw new BadRequestException("当前优惠券不存在或者在使用中!");
        }

        //2.


    }


}
