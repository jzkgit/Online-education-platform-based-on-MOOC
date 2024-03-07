package com.tianji.remark.controller;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import com.tianji.remark.service.impl.LikedRecordRedisServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * 点赞记录表 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
@Api(tags = "点赞业务相关接口")
public class LikedRecordController {


    final LikedRecordRedisServiceImpl likedRedisService;


    @ApiOperation("点赞或取消点赞服务")
    @PostMapping
    public void likeOrUnlike(@RequestBody @Validated LikeRecordFormDTO recordFormDTO){

        likedRedisService.likeOrUnlike(recordFormDTO);
    }


    @ApiOperation("查询当前用户是否点赞了指定的业务")
    @GetMapping("/list")
    public Set<Long> queryWhetherLiked(@RequestParam("bizIds") List<Long> bizIds){

        return likedRedisService.queryWhetherLiked(bizIds);
    }

}
