package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学霸天梯榜 服务实现类
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {


}
