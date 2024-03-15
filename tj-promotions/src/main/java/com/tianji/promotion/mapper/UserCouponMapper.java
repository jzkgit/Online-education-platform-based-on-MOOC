package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.enums.UserCouponStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {




}
