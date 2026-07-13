package com.joao.storemanagement.security;

import com.joao.storemanagement.entity.security.SystemUser;

public final class AuthContext {

    private static final ThreadLocal<SystemUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(SystemUser user) {
        CURRENT_USER.set(user);
    }

    public static SystemUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
