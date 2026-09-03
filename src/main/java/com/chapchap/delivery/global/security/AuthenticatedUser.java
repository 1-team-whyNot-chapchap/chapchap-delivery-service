package com.chapchap.delivery.global.security;

import com.chapchap.delivery.domain.access.constant.UserRole;

public record AuthenticatedUser(
    Long userId
    , UserRole role
) {
}