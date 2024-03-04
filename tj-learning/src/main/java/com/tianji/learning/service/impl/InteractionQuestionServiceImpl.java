package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 互动提问的问题表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {


    final InteractionQuestionServiceImpl questionService;

    final InteractionReplyServiceImpl replyService;

    final InteractionQuestionMapper questionMapper;

    final InteractionReplyMapper replyMapper;

    final UserClient userClient;



    /**
     * 新增互动问题服务
     * @param formDTO
     */
    @Override
    public void savaQuestions(QuestionFormDTO formDTO) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();

        //2.封装 po 对象
        InteractionQuestion interactionQuestion = BeanUtils.copyProperties(formDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(userId)
                .setStatus(QuestionStatus.UN_CHECK)
                .setCreateTime(LocalDateTime.now());

        //3.保存对象
        boolean save = questionService.save(interactionQuestion);
        if(!save){
            throw new DbException("新增互动问题失败!");
        }
    }


    /**
     * 修改互动问题服务
     * @param id 互动问题的ID
     */
    @Override
    public void updateQuestionsById(QuestionFormDTO formDTO,Long id) {

        //1.获取当前用户 ID
        Long userId = UserContext.getUser();

        //1.1进行校验
        if (StringUtils.isBlank(formDTO.getTitle())||StringUtils.isBlank(formDTO.getDescription())||formDTO.getAnonymity()==null){
            throw new BadRequestException("非法参数!");
        }

        //2.根据ID获取需要修改的问题内容
        LambdaQueryChainWrapper<InteractionQuestion> wrapper = questionService.lambdaQuery()
                .eq(InteractionQuestion::getId,id);
        InteractionQuestion interactionQuestion = questionMapper.selectOne(wrapper);
        if(interactionQuestion==null){
            throw new DbException("获取问题内容失败!");
        }
        //2.2 判断是否为自己的问题内容
        if(userId.equals(interactionQuestion.getUserId())){
            throw new BadRequestException("不能修改别人的问题内容!");
        }

        //3.封装 po
        BeanUtils.copyProperties(formDTO,interactionQuestion);

        //4.修改问题内容
        boolean update = questionService.updateById(interactionQuestion);
        if(!update){
            throw new DbException("修改问题内容失败!");
        }

    }


    /**
     * 分页查询问题内容服务——用户端
     * @param pageQuery
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery) {

        //1.校验 courseId
        if(pageQuery.getCourseId()==null){
            throw new BadRequestException("课程ID不能为空!");
        }

        //2.获取用户ID
        Long userId = UserContext.getUser();

        //3.获取问题PO实体类的分页数据
        Page<InteractionQuestion> questionPage = questionService.lambdaQuery()
                /*
                    由于显示的内容中，问题的描述在展示页不进行展示，所以查询的时候将描述属性给去除，节省空间
                 */
                .select(InteractionQuestion.class, new Predicate<TableFieldInfo>() {
                    @Override
                    public boolean test(TableFieldInfo tableFieldInfo) {

                        return !tableFieldInfo.getProperty().equals("description"); //指定不需要查询的字段
                    }
                })
                .eq(pageQuery.getOnlyMine(), InteractionQuestion::getUserId, userId) //判断是否查询自己的问题
                .eq(InteractionQuestion::getCourseId, pageQuery.getCourseId())
                .eq(pageQuery.getSectionId() != null, InteractionQuestion::getSectionId, pageQuery.getSectionId())
                .eq(InteractionQuestion::getHidden, false) //当前问题没有被隐藏
                .page(pageQuery.toMpPage("create_time", false)); //根据创建时间降序排列

        //3.1判断当前分页集合是否为空
        List<InteractionQuestion> records = questionPage.getRecords();
        if(records==null){
            return PageDTO.empty(questionPage); //返回空，避免抛空指针异常
        }


        //4.根据【最近回答的ID】，获取最新回答的信息
        //4.1 获取最近回答和用户的ID
        ArrayList<Long> latestAnswerIds = new ArrayList<>(); //回答ID集合
        ArrayList<Long> userIds = new ArrayList<>(); //用户ID集合
        records.forEach(new Consumer<InteractionQuestion>() {
            @Override
            public void accept(InteractionQuestion interactionQuestion) {
                if(!interactionQuestion.getAnonymity()){
                    userIds.add(interactionQuestion.getUserId());
                }
                if(interactionQuestion.getLatestAnswerId()!=null){
                    latestAnswerIds.add(interactionQuestion.getLatestAnswerId());
                }
            }
        });

        //4.2 将对应的list转换为 map 集合，方便在另一个 list 中赋值
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if(CollUtils.isNotEmpty(latestAnswerIds)) {
            List<InteractionReply> interactionReplies = replyService.list(Wrappers.<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getId, latestAnswerIds)
                    .eq(InteractionReply::getHidden, false)); //当前回答未被隐藏
//            List<InteractionReply> interactionReplies = replyService.listByIds(latestAnswerIds);
            replyMap = interactionReplies.stream()
                    .collect(Collectors.toMap(InteractionReply::getId, c -> c)); //【键：问题ID  值：问题对象】
        }

        //5.进行远程调用，获取用户的信息
        //5.1 转为 map 集合
        Map<Long, UserDTO> userDTOMap=new HashMap<>();
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        userDTOMap = userDTOS.stream()
                .collect(Collectors.toMap(UserDTO::getId, c -> c)); //【键：用户ID  值：用户对象】


        //6.遍历分页数据，进行封装 VO
        List<QuestionVO> questionVOS = new ArrayList<>();

        Map<Long, InteractionReply> finalReplyMap = replyMap; //回答信息集合
        Map<Long, UserDTO> finalUserDTOMap = userDTOMap; //用户信息集合

        records.forEach(interactionQuestion -> {

                    QuestionVO questionVO = BeanUtils.copyBean(interactionQuestion, QuestionVO.class);
                    //6.1 判断是否为匿名状态
                    if(!interactionQuestion.getAnonymity()) {
                        UserDTO userDTO = finalUserDTOMap.get(interactionQuestion.getUserId());
                        if(userDTO!=null) {
                            questionVO.setUserName(userDTO.getName());
                            questionVO.setUserIcon(userDTO.getIcon());
                        }
                    }

            InteractionReply interactionReply = finalReplyMap.get(interactionQuestion.getLatestAnswerId());
            //6.2 进行判断是否为空
            if(interactionReply!=null) {
                //6.3 非匿名采用显示名称
                if(!interactionReply.getAnonymity()) {
                    UserDTO userDTO = finalUserDTOMap.get(interactionReply.getUserId());
                    questionVO.setLatestReplyUser(userDTO.getName()); //最新的回答者昵称
                }
                questionVO.setLatestReplyContent(interactionReply.getContent()); //最新的回答信息内容
            }
                    questionVOS.add(questionVO);
                });

        return PageDTO.of(questionPage,questionVOS);
    }


    /**
     * 查询问题详情——用户端
     */
    @Override
    public QuestionVO queryQuestionInfoById(Long id) {

        QuestionVO questionVO = new QuestionVO();

        //1 判断内容是否为空
        InteractionQuestion question=null;
        if(id!=null){
            question = questionService.lambdaQuery()
                    .eq(InteractionQuestion::getId, id)
                    .one();
            if(question==null||!question.getHidden()){
                throw new BadRequestException("问题不存在或被管理员隐藏!");
            }
            //1.1 进行 VO 部分赋值
            BeanUtils.copyProperties(question,questionVO);
        }else {
            throw new BadRequestException("非法参数");
        }


        //2.根据用户ID，获取用户信息
        UserDTO userDTO = userClient.queryUserById(question.getUserId());
        if(userDTO!=null){
            //2.1 判断是否匿名
            if(!question.getAnonymity()) {
                //2.2 赋值名称、头像信息
                questionVO.setUserName(userDTO.getName());
                questionVO.setUserIcon(userDTO.getIcon());
            }
        }


        //3.获取最近回答的消息
        InteractionReply reply = replyService.lambdaQuery()
                .eq(InteractionReply::getId, question.getLatestAnswerId())
                .one();
        if(reply!=null) {
            questionVO.setLatestReplyContent(reply.getContent());

            //4. 远程调用，获得最近回答人的名称
            UserDTO userById = userClient.queryUserById(reply.getUserId());
            questionVO.setLatestReplyUser(userById.getName());
        }

        return questionVO;
    }



    /**
     * 删除我的问题
     * @param id
     */
    @Override
    public void deleteMyQuestion(Long id) {

        //1.查询当前问题是否存在
        InteractionQuestion question = questionService.lambdaQuery()
                .eq(id != null, InteractionQuestion::getId, id)
                .one();
        if(question==null){
            throw new BadRequestException("当前问题不存在!");
        }


        //2.查询当前问题是否为自己提问
        //2.1 获取当前用户的ID
        Long userId = UserContext.getUser();
        if(!question.getUserId().equals(userId)){
            throw new BadRequestException("当前问题不是自己提问的问题，删除失败!");
        }


        //3.删除该问题，以及问题下的所有评论以及回答
        int delete = questionMapper.deleteById(id);
        if(delete==0){
            throw new DbException("删除失败!");
        }
        //3.1 查询当前问题下的评论以及回答信息
        List<InteractionReply> interactionReplies = replyService.lambdaQuery()
                .eq(InteractionReply::getQuestionId, id)
                .list();

        if(interactionReplies!=null) {
            ArrayList<Long> replyIds = new ArrayList<>();
            interactionReplies.forEach(interactionReply -> replyIds.add(interactionReply.getId()));
            int deleteBatchIds = replyMapper.deleteBatchIds(replyIds);
            if(deleteBatchIds==0){
                throw new DbException("删除回答以及评论失败!");
            }
        }

    }


}
