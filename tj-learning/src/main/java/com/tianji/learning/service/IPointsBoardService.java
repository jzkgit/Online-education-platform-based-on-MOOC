package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * 学霸天梯榜 服务类
 */
public interface IPointsBoardService extends IService<PointsBoard> {


    /**
     * 查询学霸积分天梯榜
     */
    PointsBoardVO queryStudyBoard(PointsBoardQuery boardQuery);


    /**
     * 查询所有人【当前】赛季的排行信息(使用redis)
     */
    public List<PointsBoard> queryCurrentBoard(String recordKey, PointsBoardQuery boardQuery);

}
