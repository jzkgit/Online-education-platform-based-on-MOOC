package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.impl.CouponServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 优惠券的规则信息 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Api(tags = "优惠券相关接口")
public class CouponController {


    final ICouponService couponService;


    @ApiOperation("新增优惠券功能")
    @PostMapping
    public void saveCoupon(@RequestBody @Validated CouponFormDTO couponFormDTO){

        couponService.saveCoupon(couponFormDTO);
    }


    @ApiOperation("分页查询优惠券页表——管理端")
    @GetMapping(value = "/page")
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery couponQuery){

        return couponService.queryCouponPage(couponQuery);
    }



    @ApiOperation("根据ID查询优惠券信息")
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@PathVariable("id") Long id){

        return couponService.queryCouponById(id);
    }


    @ApiOperation("修改优惠券信息")
    @PutMapping("/{id}")
    public void updateCouponById(@RequestBody @Validated CouponFormDTO couponFormDTO,@PathVariable("id")Long id){

        couponService.updateCouponById(couponFormDTO,id);
    }


    @ApiOperation("删除对应的优惠券")
    @DeleteMapping("/{id}")
    public void deleteCouponById(@PathVariable("id")Long id){

        couponService.deleteCouponById(id);
    }


    @ApiOperation("发放优惠券")
    @PutMapping("/{id}/issue")
    public void issueCoupons(@PathVariable("id")Long id, @RequestBody @Validated CouponIssueFormDTO couponIssueFormDTO){

        couponService.issueCoupons(id,couponIssueFormDTO);
    }

}
