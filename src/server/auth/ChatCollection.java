package server.auth;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatCollection {

    // Nested class for a chat message
    public static class ChatMessage {
        private final String username;
        private final String message;
        private final String  timestamp;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public ChatMessage(String username, String message) {
            this.username = username;
            this.message = message;
            this.timestamp = LocalDateTime.now().format(formatter);

        }

        public String getUsername() {
            return username;
        }

        public String getMessage() {
            return message;
        }

        public String  getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return username + ": " + message;
        }
    }

    // Thread-safe list of messages
    private static final List<ChatMessage> messages = Collections.synchronizedList(new ArrayList<>());

    // Add new message
    public static void addMessage(String username, String message) {
        messages.add(new ChatMessage(username, message));
    }

    // Return all messages
    public static List<ChatMessage> getAllMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages); // copy for thread safety
        }
    }

    // Convert to simple string form for UI or log
    public static String getHistoryAsText() {
        StringBuilder sb = new StringBuilder();
        synchronized (messages) {
            for (ChatMessage msg : messages) {
                sb.append(msg.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
