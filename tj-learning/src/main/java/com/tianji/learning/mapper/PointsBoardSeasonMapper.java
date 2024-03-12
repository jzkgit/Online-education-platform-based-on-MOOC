package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 *  Mapper 接口
 */
public interface PointsBoardSeasonMapper extends BaseMapper<PointsBoardSeason> {


    /**
     * 根据传入的表名，创建对应赛季的表
     * @param tableName 传入拼接好的表名
     */
    @Select("create table `${tableName}`}\n" +
            "")
    void createPointsBoardTable(@Param("tableName") String tableName);

}
