package com.shopping.security;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private final Long id;
    private final String name;
    private final String role;

    public UserPrincipal(Long id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
