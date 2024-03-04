package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

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


    /**
     * 分页查询问题内容服务——用户端
     * @param pageQuery
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery);


    /**
     * 查询问题详情——用户端
     * @param id
     */
    QuestionVO queryQuestionInfoById(Long id);


    /**
     * 删除我的问题
     */
    void deleteMyQuestion(Long id);


    /**
     * 分页查询问题列表————管理端
     * @param adminPageQuery
     */
    PageDTO<QuestionAdminVO> queryQuestionsByPage(QuestionAdminPageQuery adminPageQuery);

}
