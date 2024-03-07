package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 点赞记录表 服务实现类 【将点赞数据加入 redis】
 */
@Service
@RequiredArgsConstructor
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {


    final LikedRecordRedisServiceImpl likedRecordService;

    final RabbitMqHelper mqHelper;

    final StringRedisTemplate redisTemplate;


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

        //2.2 获取对应业务下的点赞总数
        String likeKey = RedisConstants.LIKES_BIZ_KEY_PREFIX + recordFormDTO.getBizId();
        Long likes = redisTemplate.opsForSet().size(likeKey);
        if(likes==null){
            return;
        }

        //TODO 使用 redis 中 ZSet 数据类型进行封装值（键：【业务类型】，值：对应类型的【业务ID】以及对应的【总点赞数】）
        //2.3 拼接 ZSet 中的 key
        String typeKey = RedisConstants.LIKES_TIMES_KEY_PREFIX + recordFormDTO.getBizType();
        //2.4 封装 ZSet
        redisTemplate.opsForZSet().add(typeKey, recordFormDTO.getBizId().toString(), likes);


        /*
            【以下是使用数据库查询（被注释）】
         */
        //3.统计当前业务下的总点赞数
//        Integer count = likedRecordService.lambdaQuery()
//                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
//                .count();

        //4.将数据通过 MQ 进行传输【对应业务的总点赞数】
        //4.1 进行封装消息对象

//        LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
//        likedTimesDTO.setBizId(recordFormDTO.getBizId());
//        likedTimesDTO.setLikedTimes(likes.intValue());
//        //4.2 进行 routingKey 的动态拼接
//        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, recordFormDTO.getBizType());
//
//        log.debug("发送点赞消息 ");
//        mqHelper.send(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,  //交换机
//                routingKey, //交换机与队列连接的渠道，key
//                likedTimesDTO //进行发送的消息内容
//        );

    }


    /**
     * 查询当前用户是否点赞了指定的业务
     * @param bizIds
     */
    @Override
    public Set<Long> queryWhetherLiked(List<Long> bizIds) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2. 遍历传入的业务ID，判断当前用户是否点赞了该业务
        Set<Long> bizIdSet = new HashSet<>();
        if(CollUtils.isEmpty(bizIds)){
            return CollUtils.emptySet();
        }
        for(Long bizId : bizIds) {
            Boolean whetherExist = redisTemplate.opsForSet().isMember(RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId, userId);

            //2.1 若已经点赞，则将业务ID放入集合中保存
            if(whetherExist!=null&&!whetherExist){
                bizIdSet.add(bizId);
            }
        }

        //3.返回当前用户点赞过的业务ID集合
        if(CollUtils.isEmpty(bizIdSet)){
            return CollUtils.emptySet();
        }
        return bizIdSet;
    }



    /**
     * 进行点赞 【这里使用 redis 中的 Set 数据结构将对应的业务 ID 与对应点过赞的用户ID 一并存入】
     */
    private boolean liked(LikeRecordFormDTO recordFormDTO,Long userId) {

        /*
            【以下是使用数据库查询】
         */
//        //1.获取对应的点赞业务信息
//        LikedRecord record = likedRecordService.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
//                .one();
//
//        //2.判断之前是否已经点过赞
//        if(record==null){
//            //2.1 不存在，则表示没有过相应的点赞记录，进行保存对应记录到表中
//            LikedRecord likedRecord = BeanUtils.copyBean(recordFormDTO, LikedRecord.class);
//            likedRecord.setUserId(userId);
//            likedRecordService.save(likedRecord);
//            return true;
//        }
//        return false;

        //1.获取对应的业务ID，进行 key 值的拼接

        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + recordFormDTO.getBizId();

        //2.将对应业务的ID与进行点赞的用户ID进行【键值类型】保存
        Long add = redisTemplate.opsForSet().add(key, userId.toString());

        //3.由于 Set 类型不能有相同的值，由此来判断当前用户是否已经点过赞
        if(add!=null&&add>0){
            return false; //当前用户的点赞记录已经存在，表示点赞失败
        }
        return true; //不存在，则表示点赞成功
    }


    /**
     * 取消点赞
     */
    private boolean unliked(LikeRecordFormDTO recordFormDTO,Long userId) {

        /*
            【以下是使用数据库查询（被注释）】
         */
//        //1.获取对应的点赞业务信息
//        LikedRecord record = likedRecordService.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
//                .eq(LikedRecord::getBizType, recordFormDTO.getBizType())
//                .one();
//
//        //2.判断之前是否已经点过赞
//        //2.1 之前没有进行点赞
//        if(record==null){
//            return false;
//        }
//
//        //2.2 若之前已经点过赞，则将对应的点赞记录删除
//        boolean remove = likedRecordService.removeById(record.getId());
//        if(!remove){
//            throw new DbException("取消点赞失败!");
//        }
//        return true;

        //1.获取对应的业务 ID

        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX+recordFormDTO.getBizId();

        //2.由于是取消点赞，所以删除对应的业务ID
        Long remove = redisTemplate.opsForSet().remove(key, userId.toString());

        //3.若删除成功，即取消点赞成功，否则取消点赞失败
        if(remove!=null&&remove>0){
            return true;
        }
        return false;
    }


    /**
     * 在 redis 中批量获取点赞信息，并返回给 DB 保存
     * @param type
     * @param max_biz_size
     */
    @Override
    public void readLikesAndMsg(String type, int max_biz_size) {

        //1.进行拼接业务类型的 key
        String typeKey = RedisConstants.LIKES_TIMES_KEY_PREFIX + type;

        //2. 根据分数排序，取 max_biz_size 的对应类型的业务信息
        ArrayList<LikedTimesDTO> likedTimesDTOS = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(typeKey, max_biz_size);
        if(typedTuples==null){
            return;
        }
        for ( ZSetOperations.TypedTuple<String> typedTuple:typedTuples){

            String bizId = typedTuple.getValue(); //业务ID
            Double likedTimes = typedTuple.getScore(); //总点赞数
            //2.1 判断是否为空
            if(StringUtils.isBlank(bizId)||likedTimes==null){
                continue;
            }

            //3.封装 DTO 对象
            LikedTimesDTO likedTimesDTO = LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue());
            likedTimesDTOS.add(likedTimesDTO);
        }

        //4.将消息发送到 MQ 中
        if(CollUtils.isNotEmpty(likedTimesDTOS)) {
            log.debug("发送点赞消息到 MQ 中 ");
            String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, type); //拼接 routingKey
            mqHelper.send(
                    MqConstants.Exchange.LIKE_RECORD_EXCHANGE,  //交换机
                    routingKey, //交换机与队列连接的渠道，key
                    likedTimesDTOS //进行发送的消息内容【每次发送30条】
            );
        }

    }

}
