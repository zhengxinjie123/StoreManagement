package com.joao.storemanagement.service.security;

import com.joao.storemanagement.dto.security.SystemUserSaveDTO;
import com.joao.storemanagement.vo.security.SystemRoleVO;
import com.joao.storemanagement.vo.security.SystemUserVO;

import java.util.List;

/**
 * 系统用户管理服务。
 */
public interface SystemUserService {

    /**
     * 查询全部系统用户。
     *
     * @return 用户列表
     */
    List<SystemUserVO> list();

    /**
     * 新增系统用户。
     *
     * @param request 保存参数
     * @return 用户信息
     */
    SystemUserVO create(SystemUserSaveDTO request);

    /**
     * 更新系统用户。
     *
     * @param id      用户 ID
     * @param request 保存参数
     * @return 用户信息
     */
    SystemUserVO update(Long id, SystemUserSaveDTO request);

    /**
     * 管理员重置用户密码。
     *
     * @param id          用户 ID
     * @param newPassword 新密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 查询可选角色及默认权限。
     *
     * @return 角色列表
     */
    List<SystemRoleVO> listRoles();
}
