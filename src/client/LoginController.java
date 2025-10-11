package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import shared.BaseRequest;
import shared.BaseResponse;

import java.util.function.Consumer;

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

        BaseRequest req = new BaseRequest("login").add("username", username);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> openDashboard(username));
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    private void sendRequest(String url, BaseRequest req, Consumer<BaseResponse> onSuccess) {
        try {
            String json = mapper.writeValueAsString(req);
            http.postJsonAsync(url, json,
                    respStr -> {
                        try {
                            BaseResponse resp = mapper.readValue(respStr, BaseResponse.class);
                            onSuccess.accept(resp);
                        } catch (Exception e) {
                            Platform.runLater(() -> showStatus("Invalid response", Alert.AlertType.ERROR));
                        }
                    },
                    err -> Platform.runLater(() -> showStatus("Server error", Alert.AlertType.ERROR))
            );
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openDashboard(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Dashboard.fxml"));
            Parent root = loader.load();

            // Pass the username to dashboard controller
            DashboardController controller = loader.getController();
            controller.startDashBoard(username);

            Stage stage = (Stage) usernameField.getScene().getWindow(); // reuse same stage
            stage.setScene(new Scene(root));
            stage.setTitle("S-Emulator – Dashboard");
            stage.show();

        } catch (Exception e) {
            showStatus("Failed to load dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
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
