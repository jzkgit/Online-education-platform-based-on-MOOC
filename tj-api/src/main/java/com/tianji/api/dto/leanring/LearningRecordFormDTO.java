package com.tianji.api.dto.leanring;

import com.tianji.common.validate.annotations.EnumValid;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@ApiModel(description = "学习记录表单数据")
public class LearningRecordFormDTO {

    @ApiModelProperty("小节类型：1-视频，2-考试")
    @NotNull(message = "小节类型不能为空")
    @EnumValid(enumeration = {1,2},message = "小节类型错误 只能是 1-视频 2-考试")
    private Integer sectionType;

    @ApiModelProperty("课表id")
    @NotNull(message = "课表ID不能为空")
    private Long lessonId;

    @ApiModelProperty("对应节的id")
    @NotNull(message = "节的ID不能为空")
    private Long sectionId;

    @ApiModelProperty("视频总时长，单位秒")
    private Integer duration;

    @ApiModelProperty("视频的当前观看时长，单位秒，第一次提交填0")
    private Integer moment;

    @ApiModelProperty("提交时间")
    private LocalDateTime commitTime;
}