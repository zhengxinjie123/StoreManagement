package com.joao.storemanagement.service.security;

/**
 * 用户会话维护服务。
 */
public interface UserSessionService {

    /**
     * 清理已过期的登录会话。
     *
     * @return 删除的会话数量
     */
    int cleanExpiredSessions();
}
