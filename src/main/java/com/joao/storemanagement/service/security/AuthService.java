package com.joao.storemanagement.service.security;

import com.joao.storemanagement.dto.security.CurrentUserDTO;
import com.joao.storemanagement.dto.security.ChangePasswordDTO;
import com.joao.storemanagement.dto.security.LoginRequestDTO;
import com.joao.storemanagement.dto.security.LoginResponseDTO;
import com.joao.storemanagement.entity.security.SystemUser;

/**
 * 登录认证服务。
 */
public interface AuthService {

    /**
     * 登录并签发访问令牌。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * 根据令牌查询当前用户。
     *
     * @param token 访问令牌
     * @return 当前用户
     */
    SystemUser requireUser(String token);

    /**
     * 查询当前登录用户展示信息。
     *
     * @return 当前用户展示信息
     */
    CurrentUserDTO currentUser();

    /**
     * 当前用户修改密码。
     *
     * @param request 修改密码参数
     */
    void changePassword(ChangePasswordDTO request);

    /**
     * 注销当前令牌。
     *
     * @param token 访问令牌
     */
    void logout(String token);
}
