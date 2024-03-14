package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;

/**
 * 优惠券的规则信息 服务类
 */
public interface ICouponService extends IService<Coupon> {


    /**
     * 新增优惠券功能
     */
    void saveCoupon(CouponFormDTO couponFormDTO);


    /**
     * 分页查询优惠券页表
     * @param couponQuery
     * @return
     */
    PageDTO<CouponPageVO> queryCouponPage(CouponQuery couponQuery);


    /**
     * 根据ID查询优惠券信息
     * @param id
     * @return
     */
    CouponDetailVO queryCouponById(Long id);


    /**
     * 修改优惠券信息
     * @param couponService
     * @param id
     */
    void updateCouponById(CouponFormDTO couponService, Long id);


    /**
     * 删除对应的优惠券
     * @param id
     */
    void deleteCouponById(Long id);


    /**
     * 发放优惠券
     * @param id
     * @param couponIssueFormDTO
     */
    void issueCoupons(Long id, CouponIssueFormDTO couponIssueFormDTO);

}
