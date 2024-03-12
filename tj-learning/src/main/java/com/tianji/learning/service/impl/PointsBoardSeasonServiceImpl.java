package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {


    /**
     * 创建上赛季的榜单表
     * @param id 赛季ID
     */
    @Override
    public void createPointsBoardLatestTable(Integer id) {

        getBaseMapper().createPointsBoardTable(LearningConstants.POINTS_BOARD_TABLE_PREFIX + id);
    }

}
