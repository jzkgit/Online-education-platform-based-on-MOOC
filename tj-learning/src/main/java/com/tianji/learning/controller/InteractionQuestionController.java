package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.impl.InteractionQuestionServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 互动提问的问题表 前端控制器
 */
@RestController
@RequestMapping("/questions")
@Api(tags = "互动问答的相关接口————用户端")
@RequiredArgsConstructor
public class InteractionQuestionController {

    final InteractionQuestionServiceImpl questionService;

    @ApiOperation("新增互动问题服务")
    @PostMapping
    public void savaQuestions(@RequestBody @Validated QuestionFormDTO formDTO){

        questionService.savaQuestions(formDTO);
    }


    @ApiOperation("修改互动问题服务")
    @PutMapping("/{id}")
    public void updateQuestionsById(@RequestBody QuestionFormDTO formDTO,@PathVariable("id") Long id){

        questionService.updateQuestionsById(formDTO,id);
    }


    @ApiOperation("分页查询问题内容服务——用户端")
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery){

        return questionService.queryQuestionPage(pageQuery);
    }


    @ApiOperation("查询问题详情——用户端")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionInfoById(@PathVariable("id")Long id){

        return questionService.queryQuestionInfoById(id);
    }


    @ApiOperation("删除我的问题")
    @DeleteMapping("/{id}")
    public void deleteMyQuestion(@PathVariable("id")Long id){

        questionService.deleteMyQuestion(id);
    }


}
