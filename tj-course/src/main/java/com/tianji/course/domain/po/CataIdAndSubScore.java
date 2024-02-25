package com.tianji.course.domain.po;

import lombok.Data;

/**
 * 查询某个课程中练习对应的练习id和该练习对应的题目id
 **/
@Data
public class CataIdAndSubScore {
    //目录id
    private Long cataId;
    //题目id
    private Long subjectId;
    //题目对应的分
    private Integer score;
}
