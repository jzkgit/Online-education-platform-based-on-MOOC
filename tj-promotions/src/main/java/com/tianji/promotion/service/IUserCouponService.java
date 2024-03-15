package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 */
public interface IUserCouponService extends IService<UserCoupon> {


    /**
     * 领取优惠券
     * @param id
     */
    void receiveCoupon(Long id);


    /**
     * 兑换码兑换优惠券
     * @param code
     */
    void exchangeCoupon(String code);

}
