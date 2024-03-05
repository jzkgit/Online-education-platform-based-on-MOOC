package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.impl.InteractionReplyServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 互动问题的回答或评论 前端控制器
 */
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
@Api(tags = "互动问答相关接口 ")
public class InteractionReplyController {


    final InteractionReplyServiceImpl replyService;


    @ApiOperation("隐藏或显示评论————管理端")
    @PutMapping("/{id}/hidden/{hidden}")
    public void whetherHiddenComment(@PathVariable("id")Long id,@PathVariable("hidden")boolean hidden){

        replyService.whetherHiddenComment(id,hidden);
    }


    @ApiOperation("新增评论或回答")
    @PostMapping
    public void addCommentOrReply(ReplyDTO replyDTO){

        replyService.addCommentOrReply(replyDTO);
    }


}
