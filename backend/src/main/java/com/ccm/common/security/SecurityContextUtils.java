package com.ccm.common.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object uid = jwt.getClaim("uid");
            if (uid instanceof Number number) {
                return number.longValue();
            }
            if (uid instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Set<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> current = getCurrentRoles();
        for (String role : roles) {
            if (current.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
