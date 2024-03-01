package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;

/**
 * 互动提问的问题表 服务类
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {


    /**
     * 新增互动问题服务
     * @param formDTO
     */
    void savaQuestions(QuestionFormDTO formDTO);


    /**
     * 修改互动问题服务
     * @param id
     */
    void updateQuestionsById(QuestionFormDTO formDTO,Long id);


}
