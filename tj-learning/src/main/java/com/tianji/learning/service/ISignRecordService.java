package com.tianji.learning.service;


import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

/**
 * 签到服务
 */
public interface ISignRecordService {


    /**
     * 签到服务
     */
    SignResultVO addSignRecords();


    /**
     * 查询当前用户的签到记录
     */
    List<Long> querySignRecords();

}
