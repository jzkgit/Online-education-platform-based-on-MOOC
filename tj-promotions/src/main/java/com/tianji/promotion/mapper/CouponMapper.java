package com.tianji.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.domain.po.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 优惠券的规则信息 Mapper 接口
 */
public interface CouponMapper extends BaseMapper<Coupon> {


    /**
     * 更新优惠券数量
     * @param couponId
     */
    @Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId} AND issue_num < total_num") //原子性
    int incrIssueNum(@Param("couponId") Long couponId);



}
