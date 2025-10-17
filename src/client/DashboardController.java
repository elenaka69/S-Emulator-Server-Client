package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.*;
import shared.BaseRequest;
import shared.BaseResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;



public class DashboardController {

    private static final int REFRESH_INTERVAL = 1; // seconds
    private final HttpService http = new HttpService(); // your async HTTP helper
    private final ObjectMapper mapper = new ObjectMapper();


    private ScheduledExecutorService scheduler;

    @FXML public Label usernameField;
    @FXML private Label statusBar;
    @FXML private TextField creditsField;
    @FXML private TextField chargeAmountField;
    @FXML private ProgressBar progressBar;
    @FXML private TextField filePathField;
    @FXML public HBox mainContentBox;
    @FXML public TableView<ConnectedUsersRow> connectedUsersTable;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colNumber;
    @FXML public TableColumn<ConnectedUsersRow, String> colUsername;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colUpProg;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colUpFuncs;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colBalance;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colSpentCredits;
    @FXML public TableColumn<ConnectedUsersRow, Integer> colNumExecutions;
    @FXML public TableView<StatisticUserRow>  statisticTable;
    @FXML public TableColumn<StatisticUserRow, Integer> colStatNumber;
    @FXML public TableColumn<StatisticUserRow, String> colStatType;
    @FXML public TableColumn<StatisticUserRow, String> colStatName;
    @FXML public TableColumn<StatisticUserRow, String> colStatArch;
    @FXML public TableColumn<StatisticUserRow, Integer> colStatDegree;
    @FXML public TableColumn<StatisticUserRow, Integer> colStatResult;
    @FXML public TableColumn<StatisticUserRow, Integer> colStatCycles;

    public TableView<ProgramsRow> programsTable;
    @FXML private TableColumn<ProgramsRow, Integer> colProgNumber;
    @FXML private TableColumn<ProgramsRow, String> colProgramName;
    @FXML private TableColumn<ProgramsRow, String> colProgramUserName;
    @FXML private TableColumn<ProgramsRow, Integer> colProgramNumInstr;
    @FXML private TableColumn<ProgramsRow, Integer> colProgMaxCost;
    @FXML private TableColumn<ProgramsRow, Integer> colProgramNumExec;
    @FXML private TableColumn<ProgramsRow, Integer> colProgramAverCost;

    public TableView<FunctionsRow> functionsTable;
    @FXML private TableColumn<FunctionsRow, Integer> colFuncNumber;
    @FXML private TableColumn<FunctionsRow, String> colFuncamName;
    @FXML private TableColumn<FunctionsRow, String> colFuncamProgram;
    @FXML private TableColumn<FunctionsRow, String> colPFuncUserName;
    @FXML private TableColumn<FunctionsRow, Integer> colFuncNumInstr;
    @FXML private TableColumn<FunctionsRow, Integer> colFuncMaxCost;

    @FXML public Button chatButton;
    @FXML private TitledPane chatPane;
    @FXML private VBox chatBox;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInput;

    private String clientUsername;
    private final Map<String, String> userColors = new HashMap<>();
    private final Random random = new Random();

    private String selectedUser;
    private String selectedProgram = null;
    private int selectedProgramCost = 0;
    private final int SELECT_NONE = -1;
    private final int SELECT_PROGRAM = 1;
    private final int SELECT_FUNCTION = 2;
    private int typeProgramSelected = SELECT_NONE;

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
                typeProgramSelected = SELECT_PROGRAM;
                selectedProgramCost = newSel.getMaxCost(); // TODO chane to average cost?
                selectedProgram = newSel.getName();
                Platform.runLater(() -> functionsTable.getSelectionModel().clearSelection());
            } else {
                if (typeProgramSelected == SELECT_FUNCTION) return;
                selectedProgram = null;
                selectedProgramCost = 0;
                typeProgramSelected = SELECT_NONE;
            }
        });

        functionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                typeProgramSelected = SELECT_FUNCTION;
                selectedProgramCost = 0; // TODO get function cost?
                selectedProgram = newSel.getName();
                Platform.runLater(() -> programsTable.getSelectionModel().clearSelection());
            } else {
                if (typeProgramSelected == SELECT_PROGRAM) return;
                selectedProgram = null;
                selectedProgramCost = 0;
                typeProgramSelected = SELECT_NONE;
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
        loadUserStatistics();
    }


    private void onClose() {
        shutdown();
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
                        loadUserStatistics();
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
                            loadConnectedUsers();
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
        if (typeProgramSelected == SELECT_NONE) {
            showAlert("Program","⚠ Please select a program or function to execute.", Alert.AlertType.WARNING);
            return;
        }

        if (selectedProgram == null) {
            showAlert("Program","⚠ Please select a program to execute.", Alert.AlertType.WARNING);
            return;
        }
        if (typeProgramSelected == SELECT_PROGRAM) {
            if (selectedProgramCost > Integer.parseInt(creditsField.getText())) {
                showAlert("Insufficient Credits",
                        "You do not have enough credits to execute this program.\n" +
                                "Program Cost: " + selectedProgramCost + "\n" +
                                "Your Credits: " + creditsField.getText(),
                        Alert.AlertType.WARNING);
                return;
            }
        }

        openExecution();
    }

    private void openExecution() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Execution.fxml"));
            Parent root = loader.load();

            // Pass the username to dashboard controller
            ExecutionController controller = loader.getController();
            controller.startExecutionBoard(clientUsername, selectedProgram, typeProgramSelected == SELECT_PROGRAM, Integer.parseInt(creditsField.getText()));

            Stage stage = (Stage) usernameField.getScene().getWindow(); // reuse same stage
            stage.setScene(new Scene(root));
            stage.setTitle("S-Emulator – Execution");
            stage.show();

        } catch (Exception e) {
            showStatus("Failed to load execution: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    public void onLogout(ActionEvent actionEvent) {
        logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow(); // reuse same stage
            stage.setScene(new Scene(root));
            stage.setTitle("S-Emulator – Login");
            stage.show();

        } catch (Exception e) {
            showStatus("Failed to load login: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    public void onSendMessage(ActionEvent actionEvent) {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        BaseRequest req = new BaseRequest("sendMessage")
                .add("username", clientUsername)
                .add("message", message);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {
                     addMessage(clientUsername, message, "now");
                    chatInput.clear();
                });
            } else {
                Platform.runLater(() -> showStatus("Failed to send message: " + response.message, Alert.AlertType.ERROR));
            }
        });
    }

    public void onToggleChat(ActionEvent actionEvent) {
        boolean isVisible = chatPane.isVisible();
        Stage stage = (Stage) mainContentBox.getScene().getWindow();
        if (isVisible) {
            chatButton.setText("Chat");
            stage.setWidth(stage.getWidth() - 400);
        }
        else {
            chatButton.setText("Hide Chat");
            stage.setWidth(stage.getWidth() + 400);
        }

        chatPane.setVisible(!isVisible);
        chatPane.setManaged(!isVisible);
    }

    public static class ConnectedUsersRow {
        private final Integer number;
        private final String userName;
        private final Integer uploadedPrograms;
        private final Integer uploadedFunctions;
        private final Integer creditBalance;
        private final Integer spentCredits;
        private final Integer executions;

        public ConnectedUsersRow(int number, String userName, Integer uploadedPrograms,
                                 Integer uploadedFunctions, Integer creditBalance, Integer spentCredits, Integer executions) {
            this.number = number;
            this.userName = userName;
            this.uploadedPrograms = uploadedPrograms;
            this.uploadedFunctions = uploadedFunctions;
            this.creditBalance = creditBalance;
            this.spentCredits = spentCredits;
            this.executions = executions;
        }

        public String getUserName() { return userName; }
        public Integer getNumber() { return number; }
        public Integer getUploadedPrograms() { return uploadedPrograms; }
        public Integer getUploadedFunctions() { return uploadedFunctions; }
        public Integer getExecutions() { return executions; }
        public Integer getCreditBalance() { return creditBalance; }
        public Integer getSpentCredits() { return spentCredits; }
    }

    public static class StatisticUserRow {
        private final int number;
        private final String type;
        private final String name;
        private final String arch;
        private final int degree;
        private final int result;
        private final int cycles;

        public StatisticUserRow(int number, String type, String name, String arch,
                                int degree, int result, int cycles) {
            this.number = number;
            this.type = type;
            this.name = name;
            this.arch = arch;
            this.degree = degree;
            this.result = result;
            this.cycles = cycles;
        }

        public int getNumber() { return number; }
        public String getType() { return type; }
        public String getName() { return name; }
        public String getArch() { return arch; }
        public int getDegree() { return degree; }
        public int getResult() { return result; }
        public int getCycles() { return cycles; }
    }

    public class ProgramsRow {
        private final int number;
        private final String name;
        private final String userName;
        private final int numInstructions;
        private final int maxCost;
        private final int numExec;
        private final int averCost;

        public ProgramsRow(int number, String name, String userName,
                           int numInstructions, int maxCost, int numExec, int averCost) {
            this.number = number;
            this.name = name;
            this.userName = userName;
            this.numInstructions = numInstructions;
            this.maxCost = maxCost;
            this.numExec = numExec;
            this.averCost = averCost;
        }

        public int getNumber() { return number; }
        public String getName() { return name; }
        public String getUserName() { return userName; }
        public int getNumInstructions() { return numInstructions; }
        public int getMaxCost() { return maxCost; }
        public int getNumExec() { return numExec; }
        public int getAverCost() { return averCost; }
    }

    public class FunctionsRow {
        private final int number;
        private final String name;
        private final String programName;
        private final String userName;
        private final int numInstructions;
        private final int maxCost;

        public FunctionsRow(int number, String name, String programName, String userName,
                            int numInstructions, int maxCost) {
            this.number = number;
            this.name = name;
            this.programName = programName;
            this.userName = userName;
            this.numInstructions = numInstructions;
            this.maxCost = maxCost;
        }

        public int getNumber() { return number; }
        public String getName() { return name; }
        public String getProgramName() { return programName; }
        public String getUserName() { return userName; }
        public int getNumInstructions() { return numInstructions; }
        public int getMaxCost() { return maxCost; }
    }

    private void setupTables() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colUpProg.setCellValueFactory(new PropertyValueFactory<>("uploadedPrograms"));
        colUpFuncs.setCellValueFactory(new PropertyValueFactory<>("uploadedFunctions"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("creditBalance"));
        colSpentCredits.setCellValueFactory(new PropertyValueFactory<>("spentCredits"));
        colNumExecutions.setCellValueFactory(new PropertyValueFactory<>("executions"));

        colStatNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colStatType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatArch.setCellValueFactory(new PropertyValueFactory<>("arch"));
        colStatDegree.setCellValueFactory(new PropertyValueFactory<>("degree"));
        colStatResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colStatCycles.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        colProgNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colProgramName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProgramUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colProgramNumInstr.setCellValueFactory(new PropertyValueFactory<>("numInstructions"));
        colProgMaxCost.setCellValueFactory(new PropertyValueFactory<>("maxCost"));
        colProgramNumExec.setCellValueFactory(new PropertyValueFactory<>("numExec"));
        colProgramAverCost.setCellValueFactory(new PropertyValueFactory<>("averCost"));

        colFuncNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colFuncamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFuncamProgram.setCellValueFactory(new PropertyValueFactory<>("programName"));
        colPFuncUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colFuncNumInstr.setCellValueFactory(new PropertyValueFactory<>("numInstructions"));
        colFuncMaxCost.setCellValueFactory(new PropertyValueFactory<>("maxCost"));
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            loadConnectedUsers();
            loadProgramsTable();
            loadFunctionsTable();
            if (chatPane.isVisible())
                loadChatMessages();
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

    private void shutdown() {
        BaseRequest req = new BaseRequest("removeUser").add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
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
                                (Integer) u.get("uploadedPrograms"),
                                (Integer) u.get("uploadedFunctions"),
                                (Integer) u.get("creditBalance"),
                                (Integer) u.get("spentCredits"),
                                (Integer) u.get("executions")
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
                List<Map<String, Object>> statsMap = mapper.convertValue(
                        response.data.get("execStatistics"),
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                ObservableList<StatisticUserRow> rows = FXCollections.observableArrayList();

                for (Map<String, Object> u : statsMap) {
                    rows.add(new StatisticUserRow(
                            (Integer) u.get("number"),
                            (String) u.get("type"),
                            (String) u.get("name"),
                            (String) u.get("arch"),
                            (Integer) u.get("degree"),
                            (Integer) u.get("result"),
                            (Integer) u.get("cycles")
                    ));
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
                            (String) p.get("name"),
                            (String) p.get("userName"),
                            (Integer) p.get("numInstructions"),
                            (Integer) p.get("maxCost"),
                            (Integer) p.get("numExec"),
                            (Integer) p.get("averCost")
                    ));
                }

                programsTable.setItems(rows);
                loadConnectedUsers();
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
                ObservableList<FunctionsRow> rows = FXCollections.observableArrayList();
                for (Map<String, Object> f : functions) {
                    rows.add(new FunctionsRow(
                            (Integer) f.get("number"),
                            (String) f.get("name"),
                            (String) f.get("programName"),
                            (String) f.get("userName"),
                            (Integer) f.get("numInstructions"),
                            (Integer) f.get("maxCost")
                    ));
                }
                functionsTable.setItems(rows);
                loadConnectedUsers();
            });
        });
    }

    private void loadChatMessages() {
        BaseRequest req = new BaseRequest("getMessages");

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                return;
            }

            Platform.runLater(() -> {
                List<Map<String, Object>> messages = mapper.convertValue(
                        response.data.get("messages"),
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );

                displayMessages(messages);
            });
        });
    }

    private void displayMessages(List<Map<String, Object>> messages) {
        chatBox.getChildren().clear();

        for (Map<String, Object> msg : messages) {
            String username = (String) msg.get("username");
            String message = (String) msg.get("message");
            String timestamp = (String) msg.get("timestamp");
            addMessage(username, message, timestamp);
        }

        // scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    public void addMessage(String username, String message, String timestamp) {
        boolean isMine = username.equals(clientUsername);

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(250);
        msgLabel.setPadding(new Insets(5, 10, 5, 10));
        msgLabel.setStyle(
                "-fx-background-color: " + (isMine ? "#C8E6C9" : "#FFFFFF") + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );

        VBox vbox = new VBox();
        vbox.setSpacing(2);
        vbox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isMine) {
            Label userLabel = new Label(username);
            userLabel.setStyle("-fx-background-color: " + getColorForUser(username) +
                    "; -fx-text-fill: white; -fx-padding: 2 6 2 6; -fx-background-radius: 8;");
            userLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            vbox.getChildren().add(userLabel);
        }

        vbox.getChildren().add(msgLabel);
        messageContainer.getChildren().add(vbox);
        chatBox.getChildren().add(messageContainer);

        // scroll to bottom after adding message
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private String getColorForUser(String username) {
        if (userColors.containsKey(username))
            return userColors.get(username);

        // random pleasant color for each user
        String[] colors = {"#2196F3", "#9C27B0", "#FF9800", "#4CAF50", "#E91E63", "#3F51B5"};
        String color = colors[random.nextInt(colors.length)];
        userColors.put(username, color);
        return color;
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
