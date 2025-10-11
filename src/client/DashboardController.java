package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import shared.BaseRequest;
import shared.BaseResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DashboardController {

    private final HttpService http = new HttpService(); // your async HTTP helper
    private final ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService scheduler;

    @FXML public Label userName;
    @FXML private Label statusBar;
    @FXML private TextField creditsField;
    @FXML private TextField chargeAmountField;
    @FXML public TableView<ConnectedUsersRow> connectedUsersTable;
    @FXML public TableColumn colNumber;
    @FXML public TableColumn colUsername;
    @FXML public TableColumn colLoginTime;
    @FXML public TableView<StatisticUserRow>  statisticTable;
    @FXML public TableColumn colProperty;
    @FXML public TableColumn colValue;

    private String clientUsername;
    private String selectedUser;

    @FXML
    public void initialize() {
        // Allow only digits and prevent leading zeros
        chargeAmountField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Only digits and no leading zero unless it's a single zero
            if (newText.matches("0|[1-9]\\d*") || newText.isEmpty()) {
                return change;
            }
            return null;
        }));

        // Listen for selection changes
        connectedUsersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedUser = newSel.getUserName();
                loadUserStatistics(selectedUser);
            }
        });

        connectedUsersTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setOnCloseRequest(event -> {
                    onClose();
                });
            }
        });

        startScheduler();

        setupUsersTable();
        setupstatisticTable();
    }

    public void startDashBoard(String username) {
        this.clientUsername = username;
        selectedUser = username;
        userName.setText(username);
        showStatus("Logged in as: " + username, Alert.AlertType.INFORMATION);
        loadUserCredits();
        loadConnectedUsers();
    }


    private void onClose() {
        if (scheduler != null) {
            scheduler.shutdownNow(); // Stop updates when window closes
        }
    }

    @FXML
    private void onLoadFile() {

    }

    @FXML
    public void onChargeCredits(ActionEvent actionEvent) {
        String amountStr = chargeAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.WARNING, "Please enter the amount of credits to charge.").showAndWait()
            );
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                showStatus("Amount must be a positive number.", Alert.AlertType.WARNING);
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid number format.", Alert.AlertType.WARNING);
            return;
        }

        // Build request
        BaseRequest req = new BaseRequest("chargeCredits")
                .add("username", clientUsername)
                .add("amount", amount);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {
                    showStatus(response.message, Alert.AlertType.INFORMATION);
                    if (response.data != null && response.data.containsKey("newBalance")) {
                        creditsField.setText(response.data.get("newBalance").toString());
                        chargeAmountField.clear();
                    }
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    @FXML
    public void onUnselectUser(ActionEvent actionEvent) {
        connectedUsersTable.getSelectionModel().clearSelection();
        selectedUser = clientUsername;
        loadUserStatistics(selectedUser);
    }

    @FXML
    public void onExecuteProgram(ActionEvent actionEvent) {
    }

    public static class ConnectedUsersRow {
        private final Integer number;
        private final String userName;
        private final String loginTime;

        public ConnectedUsersRow(int number, String userName, String loginTime) {
            this.number = number;
            this.userName = userName;
            this.loginTime = loginTime;
        }

        public Integer getNumber() {
            return number;
        }
        public String getUserName() {
            return userName;
        }
        public String getLoginTime() {
            return loginTime;
        }
    }

    public static class StatisticUserRow {
        private final String property;
        private final String value;

        public StatisticUserRow(String property, String value) {
            this.property = property;
            this.value = value;
        }
        public String getProperty() {
            return property;
        }
        public String getValue() {
            return value;
        }
    }

    private void setupUsersTable() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colLoginTime.setCellValueFactory(new PropertyValueFactory<>("loginTime"));
    }
    private void setupstatisticTable()
    {
        colProperty.setCellValueFactory(new PropertyValueFactory<>("property"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            loadConnectedUsers();
            loadUserStatistics(selectedUser);
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void loadUserCredits() {
        BaseRequest req = new BaseRequest("getCredits").add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Object val = response.data.get("credits");
                if (val != null) {
                    Platform.runLater(() -> creditsField.setText(String.valueOf(val)));
                } else {
                    Platform.runLater(() -> showStatus("No credits field in response", Alert.AlertType.WARNING));
                }
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    @FXML
    public void loadConnectedUsers() {
        BaseRequest req = new BaseRequest("getUsers");

        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<Map<String, Object>> users = mapper.convertValue(
                            response.data.get("users"),
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    ObservableList<ConnectedUsersRow> rows = FXCollections.observableArrayList();
                    for (Map<String, Object> u : users) {
                        rows.add(new ConnectedUsersRow(
                                (Integer) u.get("number"),
                                (String) u.get("userName"),
                                (String) u.get("loginTime")
                        ));
                    }

                    connectedUsersTable.setItems(rows);
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }

            });
        });
    }

    private void loadUserStatistics(String username) {
        BaseRequest req = new BaseRequest("userStatistics").add("username", username);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                return;
            }

            Platform.runLater(() -> {
                Map<String, Object> statsMap = (Map<String, Object>) response.data.get("statistics");
                ObservableList<StatisticUserRow> rows = FXCollections.observableArrayList();

                for (Map.Entry<String, Object> entry : statsMap.entrySet()) {
                    rows.add(new StatisticUserRow(entry.getKey(), String.valueOf(entry.getValue())));
                }

                statisticTable.setItems(rows);
            });
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
