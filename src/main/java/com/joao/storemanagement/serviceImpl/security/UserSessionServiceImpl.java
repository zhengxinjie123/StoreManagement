package com.joao.storemanagement.serviceImpl.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joao.storemanagement.entity.security.UserSession;
import com.joao.storemanagement.mapper.primary.UserSessionMapper;
import com.joao.storemanagement.service.security.UserSessionService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionMapper sessionMapper;

    public UserSessionServiceImpl(UserSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public int cleanExpiredSessions() {
        return sessionMapper.delete(new LambdaQueryWrapper<UserSession>()
                .lt(UserSession::getExpiresAt, LocalDateTime.now()));
    }
}
