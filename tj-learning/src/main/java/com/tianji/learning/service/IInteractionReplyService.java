package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 互动问题的回答或评论 服务类
 */
public interface IInteractionReplyService extends IService<InteractionReply> {


    /**
     * 隐藏或显示评论————管理端
     * @param id
     * @param hidden
     */
    void whetherHiddenComment(Long id, boolean hidden);



}
