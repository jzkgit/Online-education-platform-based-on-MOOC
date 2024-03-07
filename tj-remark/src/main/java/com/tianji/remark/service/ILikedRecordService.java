package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 点赞记录表 服务类
 */
public interface ILikedRecordService extends IService<LikedRecord> {


    /**
     * 点赞或取消点赞服务
     * @param recordFormDTO
     */
    void likeOrUnlike(LikeRecordFormDTO recordFormDTO);


    /**
     * 查询当前用户是否点赞了指定的业务
     * @param bizIds
     */
    Set<Long> queryWhetherLiked(List<Long> bizIds);


    /**
     * 读取点赞信息
     * @param type
     * @param max_biz_size
     */
    void readLikesAndMsg(String type, int max_biz_size);

}
