package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.impl.InteractionQuestionServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 互动提问的问题表 前端控制器
 */
@RestController
@RequestMapping("/admin/questions")
@Api(tags = "互动问答的相关接口————管理端")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {


    final InteractionQuestionServiceImpl questionService;


    @ApiOperation("分页查询问题列表————管理端")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryQuestionsByPage(QuestionAdminPageQuery adminPageQuery){

        return questionService.queryQuestionsByPage(adminPageQuery);
    }


}
