package server.auth;

import java.util.*;

public class UserManager {

    private static final Map<String, UserProfile> activeUsers = new HashMap<>();
    private static final int INACTIVE_TIMEOUT_MS = 2 * 60000; // 2 minutes

    // -----------------------
    // Static cleanup thread
    // -----------------------
  /*  static {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(INACTIVE_TIMEOUT_MS); // 2 minutes
                    long now = System.currentTimeMillis();
                    synchronized (UserManager.class) {
                        for (UserProfile user : activeUsers.values()) {
                            if (user.isActive() && (now - user.getLastActive() > INACTIVE_TIMEOUT_MS)) {
                                user.setActive(false);
                            }
                        }
                    }
                } catch (InterruptedException ignored) {}
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }*/

    public static synchronized void addUser(String username) {
        UserProfile user = activeUsers.get(username);
        if (user == null)
            activeUsers.put(username, new UserProfile(username));
        else
            user.setActive(true);
    }

    public static synchronized void logoutUser(String username) {
        UserProfile user = activeUsers.get(username);
        if (user != null) {
            user.setActive(false);
        }
    }

    public static synchronized boolean isUserActive(String username) {
        UserProfile user = activeUsers.get(username);
        return (user != null && user.isActive());
    }

    public static synchronized Map<String, UserProfile> getActiveUsers() {
        Map<String, UserProfile> activeOnly = new HashMap<>();
        for (Map.Entry<String, UserProfile> entry : activeUsers.entrySet()) {
            if (entry.getValue().isActive()) {
                activeOnly.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(activeOnly);
    }

    public static synchronized void refreshUser(String username) {
        // Call this on any user action/heartbeat
        UserProfile user = activeUsers.get(username);
        if (user != null) user.updateLastActive();
    }

    public static void incrementPrograms(String username) {
        UserProfile user = activeUsers.get(username);
        if (user != null) user.incrementPrograms();
    }

    public static void incrementFunctions(String userName, int nFunc) {
        UserProfile user = activeUsers.get(userName);
        if (user != null) user.incrementFunctions(nFunc);
    }
}
