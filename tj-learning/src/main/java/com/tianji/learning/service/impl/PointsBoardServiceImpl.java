package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 学霸天梯榜 服务实现类
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {


    final PointsBoardServiceImpl boardService;

    final StringRedisTemplate redisTemplate;

    final UserClient userClient;


    /**
     * 查询学霸积分天梯榜
     */
    @Override
    public PointsBoardVO queryStudyBoard(PointsBoardQuery boardQuery) {

        //1.获取当前用户ID
        Long userId = UserContext.getUser();
        //1.1 拼接 redis 中的 key，用来查询用户积分信息
        LocalDateTime now = LocalDateTime.now();
        String format = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String recordKey = RedisConstants.POINTS_BOARD_KEY_PREFIX  + format; //使用时间拼接 key，以区分不同的赛季

        //2.判断是否查询本赛季的榜单（这里根据分页参数做判断）
        boolean query = boardQuery.getSeason() == null || boardQuery.getSeason() == 0;  //若为 true ，则表示查询当前赛季，即通过redis查询

        //3.查询我的榜单排名（根据DB还是 redis 进行查询，由 season 做决定）
        PointsBoard pointsBoard = null;
        if(query){
            //3.1 查询 redis
            pointsBoard = queryMyCurrentBoard(recordKey,userId); //当前赛季实时记录

        }else {
            //3.2 查询 DB
            pointsBoard = queryMyHistoryBoard(boardQuery.getSeason(),userId); //历史记录
        }
        if(pointsBoard==null){
            return null;
        }


        //4.查询所有人的榜单排名信息（根据DB还是 redis 进行查询，由 season 做决定）
        List<PointsBoard> pointsBoards = new ArrayList<>();
        pointsBoards = query?queryCurrentBoard(recordKey,boardQuery):queryHistoryBoard(boardQuery);
        if(pointsBoards==null){
            return null;
        }

        //5.进行封装 积分榜单 VO 集合
        List<Long> userIds = pointsBoards.stream().map(PointsBoard::getUserId).collect(Collectors.toList());    //获取所有用户ID集合

        //5.1进行【远程调用 用户服务】，获取用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        if(userDTOS==null){
            throw new BizIllegalException("用户不存在!");
        }
        //5.2 将用户信息集合，转换为 map 集合，以便于赋值给 list 集合
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));

        //6.封装 vo 进行返回
        List<PointsBoardItemVO> pointsBoardItemVOS = new ArrayList<>();
        for(PointsBoard board:pointsBoards){
            PointsBoardItemVO pointsBoardItemVO = new PointsBoardItemVO();
            pointsBoardItemVO.setPoints(board.getPoints());
            pointsBoardItemVO.setRank(board.getRank());
            UserDTO userDTO = userDTOMap.get(board.getUserId());
            if(userDTO!=null){
                pointsBoardItemVO.setName(userDTO.getName());
            }
            pointsBoardItemVOS.add(pointsBoardItemVO);
        }

        PointsBoardVO pointsBoardVO = new PointsBoardVO();
        pointsBoardVO.setPoints(pointsBoard.getPoints()); //自己的总积分
        pointsBoardVO.setRank(pointsBoard.getRank()); //自己的排名
        pointsBoardVO.setBoardList(pointsBoardItemVOS);

        return pointsBoardVO;
    }


    /**
     * 查询所有人【历史】赛季的排行信息 DB
     */
    private List<PointsBoard> queryHistoryBoard(PointsBoardQuery boardQuery) {

        //1.拼接将要进行查询的赛季表名（之后的 CRUD 都将在这张表中进行）
        TableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX+boardQuery.getSeason());

        //2.从 DB 中获取历史赛季排名信息
        Page<PointsBoard> boardPage = boardService.lambdaQuery()
                .eq(PointsBoard::getSeason, boardQuery.getSeason())
                .page(boardQuery.toMpPage("points", false)); //将分值进行倒序排列

        /*
        pointsBoard.setUserId(Long.valueOf(userId));
            pointsBoard.setPoints(score.intValue());
            pointsBoard.setRank(rank++);
         */
        //3.获取历史赛季信息，进行遍历封装 list 进行返回
        List<PointsBoard> records = boardPage.getRecords();
        if(records==null){
            return CollUtils.emptyList();
        }

        //3.1 将排名信息中的 rank 进行赋值
        records.forEach(new Consumer<PointsBoard>() {
                    @Override
                    public void accept(PointsBoard pointsBoard) {
                        pointsBoard.setRank(pointsBoard.getId().intValue());
                    }
                });

        return records;
    }


    /**
     * 查询所有人【当前】赛季的排行信息(使用redis)
     */
    public List<PointsBoard> queryCurrentBoard(String recordKey, PointsBoardQuery boardQuery) {

        //1.获取分页信息
        Integer pageNo = boardQuery.getPageNo(); //页码
        Integer pageSize = boardQuery.getPageSize(); //每页的大小
        int start = (pageNo-1)*pageSize;
        int end = start + pageSize -1;

        //2.从 redis 中进行按分值进行查询 ZRange
        Set<ZSetOperations.TypedTuple<String>> rangeWithScores
                = redisTemplate.opsForZSet().reverseRangeWithScores(recordKey, start, end);

        if(CollUtils.isEmpty(rangeWithScores)){
            return CollUtils.emptyList();
        }

        //3.进行遍历，封装结果集
        int rank = start + 1; //表示排名
        ArrayList<PointsBoard> pointsBoards = new ArrayList<>();
        for(ZSetOperations.TypedTuple<String> stringTypedTuple:rangeWithScores){

            String userId = stringTypedTuple.getValue(); //用户ID
            Double score = stringTypedTuple.getScore(); //用户对应的分值
            //3.1 进行判断
            if(StringUtils.isBlank(userId)||score==null){
                continue;
            }
            PointsBoard pointsBoard = new PointsBoard();
            pointsBoard.setUserId(Long.valueOf(userId));
            pointsBoard.setPoints(score.intValue());
            pointsBoard.setRank(rank++);

            pointsBoards.add(pointsBoard);
        }

        return pointsBoards;
    }


    /**
     * 通过 DB 查询【我的历史排名】信息
     */
    private PointsBoard queryMyHistoryBoard(Long season,Long userId) {

        //1.计算表名
        TableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX+season);

        //2.从 DB 中查询
        Optional<PointsBoard> pointsBoard = boardService.lambdaQuery()
                .eq(PointsBoard::getUserId, userId)
                .oneOpt();

        //3.进行封装 po 返回
        if(pointsBoard.isPresent()){

            //3.1获取数据
            PointsBoard board = pointsBoard.get();
            board.setRank(board.getId().intValue());
            return board;
        }else {
            return null;
        }
    }


    /**
     * 通过 redis 进行查询【我的实时排名】信息
     */
    private PointsBoard queryMyCurrentBoard(String recordKey,Long userId) {

        //1.根据用户ID获取对应的分值信息
        Double score = redisTemplate.opsForZSet().score(recordKey, userId.toString());  //ZScore 命令

        //2.获取当前用户的排名
        Long rank = redisTemplate.opsForZSet().reverseRank(recordKey, userId.toString()); //ZReRank 命令

        //3.封装 po 进行返回
        PointsBoard pointsBoard = new PointsBoard();
        pointsBoard.setPoints(score==null?0:score.intValue());
        pointsBoard.setRank(rank==null?0:rank.intValue()+1);   //由于从 redis 中求出来是从0开始

        return pointsBoard;
    }


}
