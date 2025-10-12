package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import shared.BaseRequest;
import shared.BaseResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;

public class DashboardController {

    private static final int REFRESH_INTERVAL = 30; // seconds
    private final HttpService http = new HttpService(); // your async HTTP helper
    private final ObjectMapper mapper = new ObjectMapper();

    private ScheduledExecutorService scheduler;

    @FXML public Label usernameField;
    @FXML private Label statusBar;
    @FXML private TextField creditsField;
    @FXML private TextField chargeAmountField;
    @FXML private ProgressBar progressBar;
    @FXML private TextField filePathField;
    @FXML public TableView<ConnectedUsersRow> connectedUsersTable;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colNumber;
    @FXML public TableColumn<ConnectedUsersRow, String> colUsername;
    @FXML public TableColumn<ConnectedUsersRow, String> colLoginTime;
    @FXML public TableView<StatisticUserRow>  statisticTable;
    @FXML public TableColumn<StatisticUserRow, String> colProperty;
    @FXML public TableColumn<StatisticUserRow, String> colValue;
    public TableView<ProgramsRow> programsTable;
    public TableColumn<ProgramsRow, Integer> colProgNumber;
    public TableColumn<ProgramsRow, String> colProgram;
    public TableColumn<ProgramsRow, Integer> colProgCost;
    public TableView<FunctionRow> functionsTable;
    public TableColumn<FunctionRow, Integer> colFuncNumber;
    public TableColumn<FunctionRow, String> colFunction;
    public TableColumn<FunctionRow, Integer> colFuncCost;


    private String clientUsername;
    private String selectedUser;

    @FXML
    public void initialize() {
        setupTables();

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
                loadUserStatistics();
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

        programsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                int cost = newSel.getCost();
                String selectedProgram = newSel.getProgramName();
                showStatus("Selected Program: " + selectedProgram + " (Cost: " + cost + " credits)", Alert.AlertType.INFORMATION);
            }
        });

        startScheduler();
    }

    public void startDashBoard(String username) {
        this.clientUsername = username;
        selectedUser = username;
        usernameField.setText(username);
        showStatus("Logged in as: " + username, Alert.AlertType.INFORMATION);
        loadUserCredits();
        loadConnectedUsers();
    }


    private void onClose() {
        logout();
        if (scheduler != null) {
            scheduler.shutdownNow(); // Stop updates when window closes
        }
    }

    @FXML
    private void onLoadFile() throws InterruptedException {
        chooseFile();
        String filePath = filePathField.getText();
        File file = validateFile(filePath);
        if (file == null) {
            filePathField.clear();
            return;
        }

        showStatus("Loading file...", Alert.AlertType.INFORMATION);
        // Simulate progress (for demo purposes)
        int steps = 200;
        new Thread(() -> {
            for (int i = 0; i <= steps; i++) {
                double progress = (double) i / steps;
                Platform.runLater(() -> progressBar.setProgress(progress));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> progressBar.setProgress(0.0));
        }).start();

        try {
            // Read file content as bytes
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            // Encode to Base64 for JSON transport
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            BaseRequest req = new BaseRequest("uploadFile")
                    .add("username", clientUsername)
                    .add("filename", file.getName())
                    .add("fileData", base64Data);

            sendRequest("http://localhost:8080/api", req, response -> {
                Platform.runLater(() -> {
                    if (response.ok) {
                        showStatus("File uploaded successfully!", Alert.AlertType.INFORMATION);
                        loadProgramsTable();
                        loadFunctionsTable();
                    } else {
                        showStatus("Upload failed: " + response.message, Alert.AlertType.ERROR);
                    }
                });
            });

        } catch (IOException e) {
            showStatus("Failed to read file: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        progressBar.setProgress(0.0);
    }

    @FXML
    public void onChargeCredits(ActionEvent actionEvent) {
        String amountStr = chargeAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            Platform.runLater(() ->
                    showAlert("Credit", "Please enter the amount of credits to charge.", Alert.AlertType.WARNING));
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
                        if (selectedUser.equals(clientUsername)) {
                            loadUserStatistics();
                        }
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
        loadUserStatistics();
    }

    @FXML
    public void onExecuteProgram(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Execution.fxml"));
            Parent root = loader.load();

            // Pass the username to dashboard controller
            ExecutionController controller = loader.getController();
         //   controller.startDashBoard(username);

            Stage stage = (Stage) usernameField.getScene().getWindow(); // reuse same stage
            stage.setScene(new Scene(root));
            stage.setTitle("S-Emulator – Execution");
            stage.show();

        } catch (Exception e) {
            showStatus("Failed to load execution: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
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
        public String getUserName() { return userName; }
        public Integer getNumber() { return number; }
        public String getLoginTime() { return loginTime; }
    }

    public static class StatisticUserRow {
        private final String property;
        private final String value;

        public StatisticUserRow(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {  return property; }
        public String getValue() { return value; }
    }

    public static class ProgramsRow {
        private final Integer number;
        private final String programName;
        private final Integer cost;

        public ProgramsRow(int number, String programName, Integer cost) {
            this.number = number;
            this.programName = programName;
            this.cost = cost;
        }
        public Integer getNumber() { return number; }
        public String getProgramName() { return programName; }
        public Integer getCost() { return cost; }
    }

    public static class FunctionRow {
        private final Integer number;
        private final String funcName;
        private final Integer cost;

        public FunctionRow(int number, String funcName, Integer cost) {
            this.number = number;
            this.funcName = funcName;
            this.cost = cost;
        }
        public Integer getNumber() { return number; }
        public String getFunctionName() { return funcName; }
        public Integer getCost() { return cost; }
    }

    private void setupTables() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colLoginTime.setCellValueFactory(new PropertyValueFactory<>("loginTime"));

        colProperty.setCellValueFactory(new PropertyValueFactory<>("property"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        colProgNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colProgram.setCellValueFactory(new PropertyValueFactory<>("programName"));
        colProgCost.setCellValueFactory(new PropertyValueFactory<>("cost"));

        colFuncNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colFunction.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        colFuncCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            loadConnectedUsers();
            loadUserStatistics();
            loadProgramsTable();
            loadFunctionsTable();
        }, 0, REFRESH_INTERVAL, TimeUnit.SECONDS);
    }

    private void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml")
        );

        // Get the current window
        Stage stage = (Stage) filePathField.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());

            showStatus("File selected: " + selectedFile.getName(), Alert.AlertType.INFORMATION);
            progressBar.setProgress(0.0);
        }
    }

    private File validateFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            showAlert("No File Selected", "Please select a file first.", Alert.AlertType.WARNING);
            return null;
        }
        if (!filePath.endsWith(".xml")) {
            showAlert("Invalid File", "Please provide a valid XML file.", Alert.AlertType.WARNING);
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            showAlert("File Not Found", "The file does not exist:\n" + filePath, Alert.AlertType.ERROR);
            return null;
        }
        return file;
    }

    private void logout() {
        BaseRequest req = new BaseRequest("logout").add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> showStatus("Logged out successfully", Alert.AlertType.INFORMATION));
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
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

    private void loadUserStatistics() {
        BaseRequest req = new BaseRequest("userStatistics").add("username", selectedUser);

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

    private void loadProgramsTable() {
        BaseRequest req = new BaseRequest("getPrograms");

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                return;
            }

            Platform.runLater(() -> {
                List<Map<String, Object>> programs = mapper.convertValue(
                        response.data.get("programs"),
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                ObservableList<ProgramsRow> rows = FXCollections.observableArrayList();
                for (Map<String, Object> p : programs) {
                    rows.add(new ProgramsRow(
                            (Integer) p.get("number"),
                            (String) p.get("programName"),
                            (Integer) p.get("cost")
                    ));
                }

                programsTable.setItems(rows);
            });
        });
    }
    private void loadFunctionsTable() {
        BaseRequest req = new BaseRequest("getFunctions");

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                return;
            }

            Platform.runLater(() -> {
                List<Map<String, Object>> functions = mapper.convertValue(
                        response.data.get("functions"),
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                ObservableList<FunctionRow> rows = FXCollections.observableArrayList();
                for (Map<String, Object> f : functions) {
                    rows.add(new FunctionRow(
                            (Integer) f.get("number"),
                            (String) f.get("functionName"),
                            (Integer) f.get("cost")
                    ));
                }

                functionsTable.setItems(rows);
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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        showStatus(message, type);
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
