package com.tianji.course.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 三级分类
 **/
@Data
@ApiModel(description = "分类")
public class CateSimpleInfoVO {
    @ApiModelProperty("一级分类")
    private Long firstCateId;
    @ApiModelProperty("一级分类名称")
    private String firstCateName;
    @ApiModelProperty("二级分类id")
    private Long secondCateId;
    @ApiModelProperty("二级分类名称")
    private String secondCateName;
    @ApiModelProperty("三级分类id")
    private Long thirdCateId;
    @ApiModelProperty("三级分类名称")
    private String thirdCateName;

}
