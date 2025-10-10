package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import shared.LoginRequest;
import shared.LoginResponse;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Label statusBar;

    private final HttpService http = new HttpService();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showStatus("Username cannot be empty", Alert.AlertType.WARNING);
            return;
        }

        LoginRequest req = new LoginRequest(username); // Only username now
        try {
            String json = mapper.writeValueAsString(req);
            showStatus("Connecting...", Alert.AlertType.INFORMATION);

            http.postJsonAsync("http://localhost:8080/login", json,
                    response -> {
                        try {
                            LoginResponse r = mapper.readValue(response, LoginResponse.class);
                            if (r.ok)
                                Session.username = username;
                            Platform.runLater(() -> showStatus(r.message, r.ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING));
                        } catch (Exception e) {
                            Platform.runLater(() -> showStatus("Invalid response", Alert.AlertType.ERROR));
                        }
                    },
                    err -> Platform.runLater(() -> showStatus("Server error", Alert.AlertType.ERROR)));

        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showStatus(String message, Alert.AlertType type) {
        statusBar.setText(message);
        if (type == Alert.AlertType.ERROR) {
            statusBar.setStyle("-fx-text-fill: red; -fx-border-color: lightgray; -fx-padding: 5;");
        } else if (type == Alert.AlertType.WARNING) {
            statusBar.setStyle("-fx-text-fill: orange; -fx-border-color: lightgray; -fx-padding: 5;");
        } else {
            statusBar.setStyle("-fx-text-fill: green; -fx-border-color: lightgray; -fx-padding: 5;");
        }
    }
}
