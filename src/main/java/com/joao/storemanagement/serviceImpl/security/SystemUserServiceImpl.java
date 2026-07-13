package com.joao.storemanagement.serviceImpl.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joao.storemanagement.dto.security.SystemUserSaveDTO;
import com.joao.storemanagement.entity.security.SystemUser;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.SystemUserMapper;
import com.joao.storemanagement.security.SystemRole;
import com.joao.storemanagement.vo.security.SystemRoleVO;
import com.joao.storemanagement.service.security.SystemUserService;
import com.joao.storemanagement.utils.PasswordHashUtil;
import com.joao.storemanagement.vo.security.SystemUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemUserServiceImpl implements SystemUserService {

    private static final String ROLE_ADMIN = SystemRole.ADMIN.code();

    private final SystemUserMapper userMapper;

    public SystemUserServiceImpl(SystemUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<SystemUserVO> list() {
        return userMapper.selectList(new LambdaQueryWrapper<SystemUser>().orderByAsc(SystemUser::getUsername))
                .stream()
                .map(SystemUserVO::of)
                .toList();
    }

    @Override
    @Transactional
    public SystemUserVO create(SystemUserSaveDTO request) {
        if (userMapper.selectOne(usernameQuery(request.username())) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("新建用户必须填写密码");
        }
        // 保存新用户并加密密码
        SystemUser user = new SystemUser();
        user.setUsername(request.username());
        user.setPasswordHash(PasswordHashUtil.hash(request.password()));
        user.setRole(normalizeRole(request.role()));
        user.setActive(request.active() == null || request.active());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return SystemUserVO.of(userMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public SystemUserVO update(Long id, SystemUserSaveDTO request) {
        SystemUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SystemUser sameNameUser = userMapper.selectOne(usernameQuery(request.username()));
        if (sameNameUser != null && !sameNameUser.getId().equals(id)) {
            throw new BusinessException("用户名已存在");
        }
        if (Boolean.TRUE.equals(user.getActive()) && ROLE_ADMIN.equals(user.getRole())) {
            boolean removeAdmin = !Boolean.TRUE.equals(request.active()) || !ROLE_ADMIN.equals(request.role());
            long activeAdminCount = userMapper.selectCount(new LambdaQueryWrapper<SystemUser>()
                    .eq(SystemUser::getRole, ROLE_ADMIN)
                    .eq(SystemUser::getActive, true));
            if (removeAdmin && activeAdminCount <= 1) {
                throw new BusinessException("至少需要保留一个启用的管理员");
            }
        }
        // 更新基础信息，密码为空时保持原密码
        user.setUsername(request.username());
        user.setRole(normalizeRole(request.role()));
        user.setActive(request.active() == null || request.active());
        user.setUpdatedAt(LocalDateTime.now());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(PasswordHashUtil.hash(request.password()));
        }
        userMapper.updateById(user);
        return SystemUserVO.of(userMapper.selectById(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SystemUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException("新密码不能为空");
        }
        // 管理员重置用户密码
        user.setPasswordHash(PasswordHashUtil.hash(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private LambdaQueryWrapper<SystemUser> usernameQuery(String username) {
        return new LambdaQueryWrapper<SystemUser>().eq(SystemUser::getUsername, username);
    }

    @Override
    public List<SystemRoleVO> listRoles() {
        return SystemRole.selectableRoles().stream().map(SystemRoleVO::of).toList();
    }

    private String normalizeRole(String role) {
        return SystemRole.require(role).code();
    }
}
