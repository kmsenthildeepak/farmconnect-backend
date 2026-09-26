package com.farmconnect.util;

import com.farmconnect.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = currentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException(
                    "Unexpected authentication principal type: "
                            + principal.getClass().getName()
            );
        }

        return (UserPrincipal) principal;
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }
}