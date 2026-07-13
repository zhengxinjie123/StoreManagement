package com.joao.storemanagement.serviceImpl.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joao.storemanagement.dto.security.ChangePasswordDTO;
import com.joao.storemanagement.dto.security.CurrentUserDTO;
import com.joao.storemanagement.dto.security.LoginRequestDTO;
import com.joao.storemanagement.dto.security.LoginResponseDTO;
import com.joao.storemanagement.entity.security.SystemUser;
import com.joao.storemanagement.entity.security.UserSession;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.SystemUserMapper;
import com.joao.storemanagement.mapper.primary.UserSessionMapper;
import com.joao.storemanagement.security.AuthContext;
import com.joao.storemanagement.security.LoginAttemptTracker;
import com.joao.storemanagement.security.MenuCatalog;
import com.joao.storemanagement.security.RolePermissionRegistry;
import com.joao.storemanagement.service.security.AuthService;
import com.joao.storemanagement.service.security.UserSessionService;
import com.joao.storemanagement.utils.PasswordHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ADMIN = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final long TOKEN_HOURS = 12;

    private final SystemUserMapper userMapper;
    private final UserSessionMapper sessionMapper;
    private final UserSessionService userSessionService;
    private final LoginAttemptTracker loginAttemptTracker;

    public AuthServiceImpl(
            SystemUserMapper userMapper,
            UserSessionMapper sessionMapper,
            UserSessionService userSessionService,
            LoginAttemptTracker loginAttemptTracker) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.userSessionService = userSessionService;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 登录前清理已过期会话
        userSessionService.cleanExpiredSessions();
        initDefaultAdminIfEmpty(request);
        loginAttemptTracker.checkAllowed(request.username());
        SystemUser user = userMapper.selectOne(new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getUsername, request.username()));
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            loginAttemptTracker.recordFailure(request.username());
            throw new BusinessException("用户名或密码错误");
        }
        if (!PasswordHashUtil.matches(request.password(), user.getPasswordHash())) {
            loginAttemptTracker.recordFailure(request.username());
            throw new BusinessException("用户名或密码错误");
        }
        loginAttemptTracker.reset(request.username());
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_HOURS);
        String token = UUID.randomUUID().toString().replace("-", "");
        // 登录成功后保存服务端会话
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setToken(token);
        session.setExpiresAt(expiresAt);
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return buildLoginResponse(token, user, expiresAt);
    }

    @Override
    public SystemUser requireUser(String token) {
        UserSession session = sessionMapper.selectOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getToken, token));
        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        SystemUser user = userMapper.selectById(session.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("登录用户不可用");
        }
        return user;
    }

    @Override
    public CurrentUserDTO currentUser() {
        SystemUser user = AuthContext.get();
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        return buildCurrentUser(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO request) {
        SystemUser user = AuthContext.get();
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        SystemUser dbUser = userMapper.selectById(user.getId());
        if (!PasswordHashUtil.matches(request.oldPassword(), dbUser.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        // 修改当前用户密码
        dbUser.setPasswordHash(PasswordHashUtil.hash(request.newPassword()));
        dbUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(dbUser);
    }

    @Override
    public void logout(String token) {
        sessionMapper.delete(new LambdaQueryWrapper<UserSession>().eq(UserSession::getToken, token));
    }

    private void initDefaultAdminIfEmpty(LoginRequestDTO request) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        if (!DEFAULT_ADMIN.equals(request.username()) || !DEFAULT_PASSWORD.equals(request.password())) {
            throw new BusinessException("系统未初始化，请使用默认管理员账号登录");
        }
        SystemUser user = new SystemUser();
        user.setUsername(DEFAULT_ADMIN);
        user.setPasswordHash(PasswordHashUtil.hash(DEFAULT_PASSWORD));
        user.setRole(ROLE_ADMIN);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    private LoginResponseDTO buildLoginResponse(String token, SystemUser user, LocalDateTime expiresAt) {
        CurrentUserDTO currentUser = buildCurrentUser(user);
        return new LoginResponseDTO(
                token,
                currentUser.username(),
                currentUser.role(),
                currentUser.permissions(),
                currentUser.menus(),
                expiresAt);
    }

    private CurrentUserDTO buildCurrentUser(SystemUser user) {
        var permissions = RolePermissionRegistry.permissionsOf(user.getRole());
        return new CurrentUserDTO(
                user.getUsername(),
                user.getRole(),
                RolePermissionRegistry.permissionCodesOf(user.getRole()),
                MenuCatalog.menusFor(permissions));
    }
}
