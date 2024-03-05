package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞记录表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {


    final LikedRecordServiceImpl likedRecordService;

    final RabbitMqHelper mqHelper;


    /**
     * 点赞或取消点赞服务
     * @param recordFormDTO
     */
    @Override
    public void likeOrUnlike(LikeRecordFormDTO recordFormDTO) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.判断是否进行点赞
        boolean flag = recordFormDTO.getLiked()?liked(recordFormDTO,userId):unliked(recordFormDTO,userId);

        //2.1 点赞或取消赞失败
        if(!flag){
            return;
        }

        //3.统计当前业务下的总点赞数
        Integer count = likedRecordService.lambdaQuery()
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
                .count();

        //4.将数据通过 MQ 进行传输【对应业务的总点赞数】
        //4.1 进行封装消息对象
        LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
        likedTimesDTO.setBizId(recordFormDTO.getBizId());
        likedTimesDTO.setLikedTimes(count);
        //4.2 进行 routingKey 的动态拼接
        String key = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, recordFormDTO.getBizType());

        log.debug("发送点赞消息 ");
        mqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,  //交换机
                key, //交换机与队列连接的渠道，key
                likedTimesDTO //进行发送的消息内容
        );
    }


    /**
     * 查询当前用户是否点赞了指定的业务
     * @param bizIds
     */
    @Override
    public Set<Long> queryWhetherLiked(List<Long> bizIds) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.查询对应的业务信息【查询出来的都是已经被点过赞的】
        List<LikedRecord> likedRecords = likedRecordService.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .in(LikedRecord::getBizId, bizIds)
                .list();

        //3.list 转 set 集合，返回业务 id 集合
        return likedRecords.stream()
                .map(LikedRecord::getBizId) //指定 set 中的变量值
                .collect(Collectors.toSet());
    }


    /**
     * 进行点赞
     */
    private boolean liked(LikeRecordFormDTO recordFormDTO,Long userId) {

        //1.获取对应的点赞业务信息
        LikedRecord record = likedRecordService.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
                .one();

        //2.判断之前是否已经点过赞
        if(record==null){
            //2.1 不存在，则表示没有过相应的点赞记录，进行保存对应记录到表中
            LikedRecord likedRecord = BeanUtils.copyBean(recordFormDTO, LikedRecord.class);
            likedRecord.setUserId(userId);
            likedRecordService.save(likedRecord);
            return true;
        }
        return false;
    }


    /**
     * 取消点赞
     */
    private boolean unliked(LikeRecordFormDTO recordFormDTO,Long userId) {

        //1.获取对应的点赞业务信息
        LikedRecord record = likedRecordService.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
                .eq(LikedRecord::getBizType, recordFormDTO.getBizType())
                .one();

        //2.判断之前是否已经点过赞
        //2.1 之前没有进行点赞
        if(record==null){
            return false;
        }

        //2.2 若之前已经点过赞，则将对应的点赞记录删除
        boolean remove = likedRecordService.removeById(record.getId());
        if(!remove){
            throw new DbException("取消点赞失败!");
        }
        return true;
    }


}
