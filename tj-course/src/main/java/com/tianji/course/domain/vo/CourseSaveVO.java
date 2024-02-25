package com.tianji.course.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "课程保存结果")
@AllArgsConstructor
@NotNull
@Builder
public class CourseSaveVO {
    @ApiModelProperty("课程id")
    private Long id;
}
