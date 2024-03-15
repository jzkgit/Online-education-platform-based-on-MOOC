package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;

/**
 * 兑换码 服务类
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {


    /**
     * 异步生成兑换码
     * @param coupon
     */
    void getExchangeCodeInfo(Coupon coupon);


    /**
     * 结合 redis 判断当前兑换码是否已经使用过
     */
    boolean updateExchangeCodeMark(long parseCode, boolean b);

}
