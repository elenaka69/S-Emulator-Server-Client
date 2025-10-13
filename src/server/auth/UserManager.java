package server.auth;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserManager {

    private static final Map<String, UserProfile> activeUsers = new ConcurrentHashMap<>();
    private static final int INACTIVE_TIMEOUT_MS = 2 * 60_000; // 2 minutes

    public static void addUser(String username) {
        activeUsers.compute(username, (key, existingUser) -> {
            if (existingUser == null) {
                return new UserProfile(username);
            } else {
                existingUser.setActive(true);
                existingUser.updateLastActive();
                return existingUser;
            }
        });
    }

    public static void logoutUser(String username) {
        UserProfile user = activeUsers.get(username);
        if (user != null) {
            user.setActive(false);
        }
    }

    public static boolean isUserActive(String username) {
        UserProfile user = activeUsers.get(username);
        return user != null && user.isActive();
    }

    public static Map<String, UserProfile> getActiveUsers() {

        Map<String, UserProfile> activeOnly = activeUsers.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        return Collections.unmodifiableMap(activeOnly);
    }


    public static void refreshUser(String username) {
        UserProfile user = activeUsers.get(username);
        if (user != null) {
            user.updateLastActive();
        }
    }

    public static void incrementPrograms(String username) {
        UserProfile user = activeUsers.get(username);
        if (user != null) {
            user.incrementPrograms();
        }
    }

    public static void incrementFunctions(String username, int nFunc) {
        UserProfile user = activeUsers.get(username);
        if (user != null) {
            user.incrementFunctions(nFunc);
        }
    }

    public static void cleanupInactiveUsers() {
        long now = System.currentTimeMillis();
        activeUsers.values().removeIf(user ->
                user.isActive() && (now - user.getLastActive() > INACTIVE_TIMEOUT_MS)
        );
    }

    public static void removeUser(String username) {
        activeUsers.remove(username);
    }
}
