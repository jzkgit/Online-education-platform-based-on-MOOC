package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static com.tianji.promotion.constants.PromotionConstants.*;

/**
 * 兑换码 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {


    final IExchangeCodeService codeService;

    final StringRedisTemplate redisTemplate;


    /**
     * 使用【线程池异步】生成兑换码
     */
    @Override
    @Async(value = "generateExchangeCodeExecutor") //异步线程名称，进行异步的执行任务
    public void getExchangeCodeInfo(Coupon coupon) {

        log.debug("生成兑换码  线程池名称：{}",Thread.currentThread().getName());

        //1.为兑换码自增 ID不重复，使用 redis 中的 incrBy 进行一次性自增（第二次的话，进行【叠加值】，以此类推...）
        //1.1 获取当前优惠券需要生成兑换码的数量
        Integer totalNum = coupon.getTotalNum();
        Long increment = redisTemplate.opsForValue().increment(COUPON_RANGE_KEY,totalNum);
        if(increment==null){
            return;
        }

        //2.调用工具类生成兑换码
        int maxNum = increment.intValue();
        int begin = maxNum - totalNum +1; //下一次兑换码编号开始的值
        ArrayList<ExchangeCode> exchangeCodes = new ArrayList<>();
        for(int i = begin;i<=maxNum;i++){
            String code = CodeUtil.generateCode(i, coupon.getId());  //使用工具类生成兑换码
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setId(i);  //手动赋值优惠券的ID
            exchangeCode.setCode(code);
            exchangeCode.setExchangeTargetId(coupon.getId()); //当前优惠券ID
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            exchangeCodes.add(exchangeCode);
        }

        //3.批量保存兑换码到 DB
        codeService.saveBatch(exchangeCodes);
    }


    /**
     * 结合 redis 判断当前兑换码是否已经使用过
     */
    @Override
    public boolean updateExchangeCodeMark(long parseCode, boolean mark) {

        //1.拼接 key
        String codeKey = COUPON_CODE_MAP_KEY;

        Boolean res = redisTemplate.opsForValue().setBit(codeKey, parseCode, mark);

        return res!=null && res;
    }


}
