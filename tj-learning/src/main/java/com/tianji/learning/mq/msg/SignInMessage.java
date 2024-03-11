package com.tianji.learning.mq.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SignInMessage {

    private Long userId; //用户ID

    private Integer points; //当前用户获取的积分数

}
