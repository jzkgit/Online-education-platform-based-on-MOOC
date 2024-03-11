package com.tianji.learning.service.impl;


import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 签到实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {


    final SignRecordServiceImpl signRecordService;

    final StringRedisTemplate redisTemplate;

    final RabbitMqHelper mqHelper;


    /**
     * 签到服务
     * @return
     */
    @Override
    public SignResultVO addSignRecords() {

        //1.获取用户ID
        Long userId = UserContext.getUser();

        //2.拼接 key（格式：xxx:xxx:userId:202403）
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern(":yyyyMM")); //得到冒号+年月
        String userKey = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString()+time;

        //3.将签到信息保存到 bitMap 中【即用户今天进行签到】
        int offset = now.getDayOfMonth() - 1;   //获取【当月的当天数】，减一匹配 bitMap 中的天数偏移量
        Boolean sign = redisTemplate.opsForValue().setBit(userKey, offset, true); //返回原来位置的二进制(0/1)
        //3.1 判断当天是否已经签过到
        if(sign!=null&&sign){
            throw new BizIllegalException("不能重复签到!");
        }

        //4.计算当月用户连续签到的天数
        int days = countSignDays(userKey,now.getDayOfMonth());

        //5.连续签到，则进行对应的积分奖励
        int rewardPoints = 0 ; //连续签到的奖励积分
        if(days==7){
            rewardPoints= 10;
        }else if(days==14){
            rewardPoints = 20;
        } else if (days == 28) {
            rewardPoints = 40;
        }

        //6.TODO 使用MQ进行异步通知，保存积分
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId,1+rewardPoints));


        //7.封装 Vo 进行返回
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(days);
        signResultVO.setRewardPoints(rewardPoints); //连续签到的积分

        return signResultVO;
    }


    /**
     * 查询当前用户的签到记录
     */
    @Override
    public List<Long> querySignRecords() {

        //1.获取当前用户的ID
        Long userId = UserContext.getUser();

        //2.使用 bitMap 中的 bitfield 获取当前用户当月的所有的签到信息
        //2.1 获取key
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern(":yyyyMM")); //得到冒号+年月
        String userKey = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString()+time;

        int dayOfMonth = now.getDayOfMonth(); //当月到当天为止的总天数

        //3.进行获取数据
        List<Long> list = redisTemplate.opsForValue().bitField(userKey,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if(list==null){
            return CollUtils.emptyList();
        }
        //3.1 将十进制转化位二进制
        Long records = list.get(0);

        //4. 将获取到的二进制，进行右移，获取每一天的签到数据
        ArrayList<Long> userRecords = new ArrayList<>();
        while (dayOfMonth!=0){
            userRecords.add(records);
            records = records>>>1;
            dayOfMonth--; //进行减减，直到最后一天
        }

        return userRecords;
    }


    /**
     * 获取用户当月连续签到的天数
     * @param userKey
     * @param dayOfMonth
     */
    private int countSignDays(String userKey, int dayOfMonth) {

        //1.求本月到今天所有签到的数据（这里使用 bitfield 得到的是十进制【这里是无符号，即值只有正数】）
        List<Long> list = redisTemplate.opsForValue().bitField(userKey,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if(CollUtils.isEmpty(list)){
            return 0;
        }

        //2.获取从第一天签到至今的签到数据
        Long records = list.get(0);
        log.debug("该用户签到至今的记录：{}",records);

        //2.1 获取 bitfield 生成的【总数据】后，将其转为二进制，然后【从后往前推】，以此【判断该用户当月连续签到了几天】
        int count = 0;
        while ((records&1)==1){ //使二进制数据逐个与 "1" 进行与运算（1：签到 0：未签到）
            count++;
            records = records >> 1; //将二进制每判断一下，整体就进行右移一位，直到当前位为 0 为止即为结束
        }

        //3.返回当前连续签到的天数
        return count;
    }


}
