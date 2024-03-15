package com.tianji.promotion.controller;

import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user-coupons")
@Api(tags = "优惠券相关接口")
public class UserCouponController {


    final IUserCouponService userCouponService;


    @ApiOperation("领取优惠券")
    @PostMapping("/{id}/receive")
    public void receiveCoupon(@PathVariable("id")Long id){

        userCouponService.receiveCoupon(id);
    }



    @ApiOperation("兑换码兑换优惠券")
    @PostMapping("/{code}/exchage")
    public void exchangeCoupon(@PathVariable("code")String code){

        userCouponService.exchangeCoupon(code);
    }


}
