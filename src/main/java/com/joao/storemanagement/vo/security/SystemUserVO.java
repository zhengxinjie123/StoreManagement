package com.joao.storemanagement.vo.security;

import com.joao.storemanagement.entity.security.SystemUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemUserVO {

    private final Long id;
    private final String username;
    private final String role;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static SystemUserVO of(SystemUser user) {
        return SystemUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
