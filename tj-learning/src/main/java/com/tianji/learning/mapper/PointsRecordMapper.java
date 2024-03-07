package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.tianji.learning.domain.po.PointsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习积分记录，每个月底清零 Mapper 接口
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {



}
