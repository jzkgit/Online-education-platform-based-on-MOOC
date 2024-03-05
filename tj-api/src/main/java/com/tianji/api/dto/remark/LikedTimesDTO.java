package com.tianji.api.dto.remark;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 用于
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class LikedTimesDTO {

    @ApiModelProperty("点赞业务id")
    @NotNull(message = "业务id不能为空")
    private Long bizId;

    @ApiModelProperty("点赞的总数")
    private Integer likedTimes;

}
