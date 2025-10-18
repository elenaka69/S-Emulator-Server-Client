package client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDateTime;

public class ChatUIHelper {

    private final VBox chatBox;
    private final ScrollPane chatScrollPane;
    private final String currentUser;
    private final Map<String, String> userColors = new HashMap<>();
    private final Random random = new Random();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ChatUIHelper(VBox chatBox, ScrollPane chatScrollPane, String currentUser) {
        this.chatBox = chatBox;
        this.chatScrollPane = chatScrollPane;
        this.currentUser = currentUser;
    }

    public void addMessage(String username, String message, String timestamp) {
        boolean isMine = username.equals(currentUser);

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Message label
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(250);
        msgLabel.setPadding(new Insets(5, 10, 5, 10));
        msgLabel.setStyle(
                "-fx-background-color: " + (isMine ? "#C8E6C9" : "#FFFFFF") + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );

        // VBox to hold message + optional username + timestamp
        VBox vbox = new VBox();
        vbox.setSpacing(2);
        vbox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Username (only for others)
        if (!isMine) {
            Label userLabel = new Label(username);
            userLabel.setStyle("-fx-background-color: " + getColorForUser(username) +
                    "; -fx-text-fill: white; -fx-padding: 2 6 2 6; -fx-background-radius: 8;");
            userLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            vbox.getChildren().add(userLabel);
        }

        // Add message text
        vbox.getChildren().add(msgLabel);

        // Timestamp label
        if (timestamp != null && !timestamp.isBlank()) {
            Label timeLabel = new Label(timestamp);
            timeLabel.setFont(Font.font("System", FontPosture.ITALIC, 9));
            timeLabel.setTextFill(Color.GRAY);
            vbox.getChildren().add(timeLabel);
        }

        messageContainer.getChildren().add(vbox);
        chatBox.getChildren().add(messageContainer);

        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }


    public void displayMessages(List<Map<String, Object>> messages) {
        chatBox.getChildren().clear();
        for (Map<String, Object> msg : messages) {
            String username = (String) msg.get("username");
            String message = (String) msg.get("message");
            String timestamp = (String) msg.get("timestamp");
            addMessage(username, message, timestamp);
        }
    }

    public void sendMyMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        addMessage(currentUser, text.trim(), LocalDateTime.now().format(formatter));
    }

    private String getColorForUser(String username) {
        if (userColors.containsKey(username))
            return userColors.get(username);

        String[] colors = {"#2196F3", "#9C27B0", "#FF9800", "#4CAF50", "#E91E63", "#3F51B5"};
        String color = colors[random.nextInt(colors.length)];
        userColors.put(username, color);
        return color;
    }
}
