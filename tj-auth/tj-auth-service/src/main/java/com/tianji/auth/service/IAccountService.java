package com.tianji.auth.service;

import com.tianji.api.dto.user.LoginFormDTO;

/**
 * 账号表，平台内所有用户的账号、密码信息 服务类
 */
public interface IAccountService{

    String login(LoginFormDTO loginFormDTO, boolean isStaff);

    void logout();

    String refreshToken(String refreshToken);

}
