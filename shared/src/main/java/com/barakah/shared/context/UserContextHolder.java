package com.barakah.shared.context;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();
    
    public static void setContext(UserContext userContext) {
        CONTEXT.set(userContext);
    }
    
    public static UserContext getContext() {
        return CONTEXT.get();
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
    
    public static String getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }
    
    public static boolean isCurrentUserAdmin() {
        UserContext context = getContext();
        return context != null && context.isAdmin();
    }
}