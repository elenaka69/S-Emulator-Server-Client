package client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import shared.BaseRequest;
import shared.BaseResponse;
import shared.ExecutionStep;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ExecutionController {
    private final HttpService http = new HttpService(); // your async HTTP helper
    private final ObjectMapper mapper = new ObjectMapper();
    
    @FXML public Label usernameField;
    @FXML public TextField creditsField;
    @FXML private Label statusBar;

    @FXML public HBox runButtonsBox;
    @FXML public VBox runBox;
    private VBox paramBox;

    @FXML private Label expandLabel;
    @FXML private TextField expandField;
    @FXML private Button expandButton;
    @FXML private Button collapseButton;
    @FXML private Button runButton;
    @FXML private Button debugButton;
    @FXML private Button stepOverButton;
    @FXML private Button stepBackButton;
    @FXML private Button stopButton;
    @FXML private Button resumeButton;
    @FXML private ComboBox<String> highlightComboBox;
    @FXML private ComboBox<String> funcsComboBox;
    @FXML private TableView<InstructionRow> instructionTable;
    @FXML private TableColumn<InstructionRow, Integer> colNumber;
    @FXML private TableColumn<InstructionRow, String> colType;
    @FXML private TableColumn<InstructionRow, String> colArch;
    @FXML private TableColumn<InstructionRow, String> colLabel;
    @FXML private TableColumn<InstructionRow, String> colInstruction;
    @FXML private TableColumn<InstructionRow, Integer> colCycle;
    @FXML private TableColumn<InstructionRow, Boolean> colBreakpoint;
    @FXML private TableView<InstructionBaseRow> historyInstrTable;
    @FXML private TableColumn<InstructionBaseRow, Integer> colHistoryNumber;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryType;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryArch;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryLabel;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryInstruction;
    @FXML private TableColumn<InstructionBaseRow, Integer> colHistoryCycle;
    @FXML private TableView<WatchDebugRow> debugTable;
    @FXML private TableColumn<WatchDebugRow, String> colVar;
    @FXML private TableColumn<WatchDebugRow, String> colValue;
    @FXML private TableView<ProgramHistoryRow> historyRunTable;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunNumber;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunDegree;
    @FXML private TableColumn<ProgramHistoryRow, String> colHistoryRunInput;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunResult;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunCycle;
    private final List<TextField> paramFields = new ArrayList<>();
    private List<String> inputVariables = new ArrayList<>();
    private List<ExecutionStep> runListMap;

    private String clientUsername;
    private String programName;
    private int currentHighlightedStep = -1;
    private int currentStepIndex = 0;
    private String highlightText = null;
    private int maxDegree;
    private int runHistoryCounter = 0;
    private final ObservableList<ProgramHistoryRow> historyRunData = FXCollections.observableArrayList();

    public void initialize() {
        setupProgramTable();
        setupHistoryTable();
        setupWatchDebugTable();
        setupProgramHistory();
        setupToolTips();
        historyRunTable.setItems(historyRunData);

        instructionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                int index = newSelection.getNumber();
                loadInstructionHistory(index);
            }
        });

        funcsComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            onFuncsSelection(newValue);
        });

        highlightComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            highlightText = newValue;
            instructionTable.refresh();
        });

        instructionTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setOnCloseRequest(event -> {
                    onClose();
                });
            }
        });
    }

    private void onClose() {
        shutdown();
    }

    private void shutdown() {
        BaseRequest req = new BaseRequest("removeUser").add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (!response.ok) {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    public void startExecutionBoard(String clientUsername, String programName, int credits) {
        creditsField.setText(String.valueOf(credits));
        this.clientUsername = clientUsername;
        this.programName = programName;
        usernameField.setText(clientUsername);
        setProgramToUser();
        showStatus("Loaded program: " + programName, Alert.AlertType.INFORMATION);
    }

    public static class WatchDebugRow {
        private final String var;
        private final long value;

        public WatchDebugRow(String var, long value) {
            this.var = var;
            this.value = value;
        }
        public String getVariable() {
            return var;
        }
        public long getValue() {
            return value;
        }
    }

    public static class ProgramHistoryRow {
        private final Integer number;
        private final Integer degree;
        private final String input;
        private final Long result;
        private final Integer cycle;

        public ProgramHistoryRow(int number, Integer degree, String input, Long result, Integer cycle) {
            this.number = number;
            this.degree = degree;
            this.input = input;
            this.result = result;
            this.cycle = cycle;
        }
        public Integer getNumber() {
            return number;
        }
        public Integer getDegree() {
            return degree;
        }
        public String getInput() {
            return input;
        }
        public Long getResult() {
            return result;
        }
        public Integer getCycle() {
            return cycle;
        }
    }

    public static class InstructionBaseRow {
        private final Integer number;
        private final String type;
        private final String arch;
        private final String label;
        private final String instruction;
        private final Integer cycle;

        public InstructionBaseRow(int number, String type, String arch, String label, String instruction, int cycle) {
            this.number = number;
            this.type = type;
            this.arch = arch;
            this.label = label;
            this.instruction = instruction;
            this.cycle = cycle;
        }

        public Integer getNumber() {
            return number;
        }
        public String getType() {
            return type;
        }
        public String getLabel() {
            return label;
        }
        public String getInstruction() {
            return instruction;
        }
        public Integer getCycle() {
            return cycle;
        }
        public String getArch() { return arch; }
    }

    public static class InstructionRow extends InstructionBaseRow{

        private boolean breakpoint = false;
        private String strDataInstruction;

        public InstructionRow(int number, String type, String arch, String label, String instruction, int cycle) {
            super(number, type, arch, label, instruction, cycle);
            strDataInstruction = (label.isEmpty() ? "   " : label) + "  " + instruction + " (" + cycle + ")";
        }

        public boolean getBreakpoint() {
            return breakpoint;
        }
        public void setBreakpoint(boolean bp) { this.breakpoint = bp; }
        public String getDataInstruction() {
            return strDataInstruction;
        }
    }

    public void onExpand(ActionEvent actionEvent) {
        int setDegree;

        setDegree = Integer.parseInt(expandField.getText().trim());

        if (setDegree >= maxDegree) {
            showAlert("Invalid degree", "Max degree is " +maxDegree, Alert.AlertType.ERROR);
            return;
        }

        historyInstrTable.getItems().clear();

        BaseRequest req = new BaseRequest("expandProgram")
                .add("username", clientUsername)
                .add("degree", 1);
        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {

                    showStatus(response.message, Alert.AlertType.INFORMATION);
                    loadProgramInstructions();
                    loadHighlightComboBox();
                    setRangeDegree(setDegree+1);
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    public void onCollapse(ActionEvent actionEvent) {
        int setDegree = 1;

        setDegree = Integer.parseInt(expandField.getText().trim());

        if (setDegree == 0 ) {
            showAlert("Invalid degree", "Min degree is 0", Alert.AlertType.ERROR);
            return;
        }

        historyInstrTable.getItems().clear();

        BaseRequest req = new BaseRequest("collapseProgram")
                .add("username", clientUsername);
        int finalSetDegree = setDegree;
        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {

                    showStatus(response.message, Alert.AlertType.INFORMATION);
                    loadProgramInstructions();
                    loadHighlightComboBox();
                    setRangeDegree(finalSetDegree - 1);
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    public void onStepBackButton(ActionEvent actionEvent) {
        if (currentStepIndex - 2 >= 0)
        {
            currentStepIndex -= 2;
            stepOverRoutine();
        }
    }

    public void onResumeDebug(ActionEvent actionEvent) {
        if (runListMap == null) return;

        while (currentStepIndex <= runListMap.size()) {
            stepOverRoutine();

            if (currentHighlightedStep == -1)
                break;
            // Optional: stop automatically if a breakpoint is reached
            InstructionRow currentRow = instructionTable.getItems().get(currentHighlightedStep);
            if (currentRow.getBreakpoint()) {
                statusBar.setText("Paused at breakpoint: " + currentRow.getLabel());
                break;
            }
        }
    }

    public void onStepOver(ActionEvent actionEvent) {
        stepOverRoutine();
    }

    public void onStopDebug(ActionEvent actionEvent) {
        setDebuggingMode(false);
    }

    public void onDebug(ActionEvent actionEvent) {
        runRoutine(success -> {
            if (success) {
                setDebuggingMode(true);
                stepOverRoutine();
            }
        });
    }

    public void onRun(ActionEvent actionEvent) {
        runRoutine(success -> {
            if (success) {
                populateDebugTable(runListMap.size() - 1);
                setDebuggingMode(false);
                setStatistics();
            }
        });
    }

    private void stepOverRoutine() {
        if(runListMap == null)
            return;

        if(currentStepIndex >= runListMap.size()){
            setStatistics();
            setDebuggingMode(false);
            showStatus("Debug finished.", Alert.AlertType.INFORMATION);
            return;
        }

        currentHighlightedStep = populateDebugTable(currentStepIndex);
        currentStepIndex++;
        if(currentStepIndex >= runListMap.size())
        {
            setStatistics();
            setDebuggingMode(false);
            showStatus("Debug finished.", Alert.AlertType.INFORMATION);
            return;
        } else {
            Platform.runLater(() -> {
                instructionTable.scrollTo(currentHighlightedStep);
                instructionTable.refresh();
            });
        }
    }

    private void runRoutine(Consumer<Boolean> callback)
    {
        if (checkAndConfirmParams()) {
            List<Long> userVars = getUserVars();
            BaseRequest req = new BaseRequest("runProgram")
                    .add("username", clientUsername)
                    .add("inputVariables", userVars);

            sendRequest("http://localhost:8080/api", req, response -> {
                boolean success = false;
                if (response.ok) {
                    runListMap = mapper.convertValue(
                            response.data.get("runListMap"),
                            new TypeReference<List<ExecutionStep>>() {}
                    );

                    if (runListMap != null && !runListMap.isEmpty()) success = true;
                }

                boolean finalSuccess = success;
                Platform.runLater(() -> {
                    showStatus(response.message, response.ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
                    callback.accept(finalSuccess);
                });
            });
        } else {
            callback.accept(false);
        }
    }

    private void enableControls(boolean isEnable)
    {
        stepOverButton.setDisable(!isEnable);
        stepBackButton.setDisable(!isEnable);
        stopButton.setDisable(!isEnable);
        resumeButton.setDisable(!isEnable);
        runButton.setDisable(isEnable);
        debugButton.setDisable(isEnable);
        expandButton.setDisable(isEnable);
        collapseButton.setDisable(isEnable);
    }

   private void setDebuggingMode(boolean active)
    {
        currentHighlightedStep = -1;
        currentStepIndex = 0;
        enableControls(active);
        instructionTable.scrollTo(0);
        instructionTable.refresh();
    }


    private void updateProgramStatisticTable(long result, int cycles)
    {
        runHistoryCounter++;

        int degree = Integer.parseInt(expandField.getText().trim());
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (TextField field : paramFields) {
            String text = field.getText();
            String varName = inputVariables.get(i++);
            long value = !text.isEmpty() ? Long.parseLong(text) : 0;
            sb.append(varName).append(" = ").append(value).append("   ");
        }

        historyRunData.add(new ProgramHistoryRow(
                runHistoryCounter,
                degree,
                sb.toString(),
                result,
                cycles
        ));
    }
    private void setStatistics() {
        BaseRequest req = new BaseRequest("getRunStatistic").add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Object resultVal = response.data.get("result");
                Object cyclesVal = response.data.get("cycles");
                long result = resultVal != null ? ((Number) resultVal).longValue() : 0;
                int cycles = cyclesVal != null ? (Integer) cyclesVal : 0;
                updateProgramStatisticTable(result,cycles);

            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });


    }

    private int populateDebugTable(int stepIndex) {
        ExecutionStep currentStep = runListMap.get(stepIndex);
        Map<String, Long> currentMap = currentStep.getVariables();

        ObservableList<WatchDebugRow> data = FXCollections.observableArrayList();

        for( Map.Entry<String, Long> entry :currentMap.entrySet()) {
            data.add(new WatchDebugRow(
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        debugTable.setItems(data);
        return currentStep.getStep();
    }

    public void onBackToDashboard(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.startDashBoard(clientUsername);

            Stage stage = (Stage) usernameField.getScene().getWindow(); // reuse same stage
            stage.setScene(new Scene(root));
            stage.setTitle("S-Emulator – Login");
            stage.show();

        } catch (Exception e) {
            showStatus("Failed to load login: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void setProgramToUser() {
        BaseRequest req = new BaseRequest("setProgramToUser")
                .add("username", clientUsername)
                .add("programName", programName);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {
                    
                    showStatus(response.message, Alert.AlertType.INFORMATION);
                    loadProgramInstructions();
                    loadHighlightComboBox();
                    loadInputVariables();
                    setRangeDegree(0);
                    loadFuncsSelection();
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    private void setupProgramTable() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colArch.setCellValueFactory(new PropertyValueFactory<>("arch"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
        colBreakpoint.setCellValueFactory(new PropertyValueFactory<>("breakpoint"));

        colBreakpoint.setCellFactory(tc -> {
            TableCell<InstructionRow, Boolean> cell = new TableCell<>() {
                @Override
                protected void updateItem(Boolean bp, boolean empty) {
                    super.updateItem(bp, empty);
                    if (empty || bp == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(bp ? "●" : "");
                        setStyle(bp ? "-fx-text-fill: red; -fx-alignment: CENTER;" : "-fx-alignment: CENTER;");
                    }
                }
            };

            cell.setOnMouseClicked(e -> {
                InstructionRow row = cell.getTableView().getItems().get(cell.getIndex());
                row.setBreakpoint(!row.getBreakpoint());
                cell.getTableView().refresh();
            });

            return cell;
        });

        instructionTable.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<>() {
                @Override
                protected void updateItem(InstructionRow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle(""); // reset style
                    } else {
                        if (getIndex() == currentHighlightedStep) {
                            setStyle("-fx-background-color: lightgreen;"); // step-over
                        } else {
                            if (highlightText != null ) {
                                String label = item.getLabel();
                                String instr = item.getInstruction();
                                boolean match =
                                        (label != null && label.matches(".*\\b" + Pattern.quote(highlightText) + "\\b.*")) ||
                                                (instr != null && instr.matches(".*\\b" + Pattern.quote(highlightText) + "\\b.*"));
                                if (match) {
                                    setStyle("-fx-background-color: #F08650; -fx-text-fill: black;"); // orange highlight
                                }
                                else {
                                    setStyle(""); // default
                                }
                            }
                        }
                    }
                }
            };

            // Add breakpoint toggle on double-click
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    InstructionRow ir = row.getItem();
                    ir.setBreakpoint(!ir.getBreakpoint());
                    instructionTable.refresh(); // redraw
                }
            });

            return row;
        });
    }
    private void setupHistoryTable() {
        colHistoryNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colHistoryType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colHistoryArch.setCellValueFactory(new PropertyValueFactory<>("arch"));
        colHistoryLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colHistoryInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colHistoryCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
    }

    private void setupWatchDebugTable() {
        colVar.setCellValueFactory(new PropertyValueFactory<>("variable"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
    }

    private void setupProgramHistory() {
        colHistoryRunNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colHistoryRunDegree.setCellValueFactory(new PropertyValueFactory<>("degree"));
        colHistoryRunInput.setCellValueFactory(new PropertyValueFactory<>("input"));
        colHistoryRunResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colHistoryRunCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
    }

    private void setupToolTips()
    {
        Tooltip stopTooltip = new Tooltip("Stop the current \n debugging session");
        stopTooltip.setShowDelay(Duration.ZERO);
        stopTooltip.setShowDuration(Duration.seconds(5));
        stopTooltip.setHideDelay(Duration.ZERO);
        stopButton.setTooltip(stopTooltip);

        Tooltip resumeTooltip = new Tooltip("Resume");
        resumeTooltip.setShowDelay(Duration.ZERO);
        resumeTooltip.setShowDuration(Duration.seconds(5));
        resumeTooltip.setHideDelay(Duration.ZERO);
        resumeButton.setTooltip(resumeTooltip);

        Tooltip stepOverTooltip = new Tooltip("Step over");
        stepOverTooltip.setShowDelay(Duration.ZERO);
        stepOverTooltip.setShowDuration(Duration.seconds(5));
        stepOverTooltip.setHideDelay(Duration.ZERO);
        stepOverButton.setTooltip(stepOverTooltip);

        Tooltip stepBackTooltip = new Tooltip("Step backward");
        stepBackTooltip.setShowDelay(Duration.ZERO);
        stepBackTooltip.setShowDuration(Duration.seconds(5));
        stepBackTooltip.setHideDelay(Duration.ZERO);
        stepBackButton.setTooltip(stepBackTooltip);
    }



    private void loadFuncsSelection() {
        BaseRequest req = new BaseRequest("getProgramFunctions")
                .add("programName", programName);

        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<String> functions = mapper.convertValue(
                            response.data.get("functions"),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    ObservableList<String> funcList = FXCollections.observableArrayList(functions);
                    funcsComboBox.setItems(funcList);
                    if (!funcList.isEmpty()) {
                        funcsComboBox.getSelectionModel().select(0);
                    }
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
    }

    private boolean checkAndConfirmParams() {
        boolean hasEmpty = paramFields.stream().anyMatch(f -> f.getText().isEmpty());

        if (!hasEmpty) {
            return true; // no empty, go ahead
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Empty Parameters");
        alert.setHeaderText("Some parameter fields are empty.");
        Label content = new Label("Empty fields will be treated as 0.\nDo you want to continue?");
        content.setWrapText(true);
        content.setAlignment(Pos.CENTER);

        alert.getDialogPane().setContent(content);

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            for (TextField field : paramFields) {
                if (field.getText().trim().isEmpty()) {
                    field.setText("0");
                }
            }
            return true; // Continue
        } else {
            return false; // Cancel
        }
    }

    private List<Long> getUserVars() {
        List<Long> userVars = new ArrayList<>();
        long value;
        for (TextField field : paramFields) {
            String text = field.getText();
            if (!text.isEmpty())
                value = Long.parseLong(text);
            else
                value = 0;
            userVars.add(value);
        }
        return userVars;
    }

    private void loadInputVariables() {
        BaseRequest req = new BaseRequest("getProgramInputVariables")
                .add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<String> variables = mapper.convertValue(
                            response.data.get("inputVariables"),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    createRunParameterFields(variables);
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
    }

    private void loadHighlightComboBox() {
        BaseRequest req = new BaseRequest("getHighlightOptions")
                .add("username", clientUsername);

        highlightComboBox.getItems().clear();
        highlightText = "none";


        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<String> highlights = mapper.convertValue(
                            response.data.get("highlightOptions"),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    ObservableList<String> highlightList = FXCollections.observableArrayList(highlights);
                    highlightComboBox.setItems(highlightList);

                    if (!highlightList.isEmpty()) {
                        highlightComboBox.getSelectionModel().select(0);
                        highlightText = highlightList.get(0);
                    }
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
        instructionTable.refresh();
    }

    private void loadProgramInstructions() {

        BaseRequest req = new BaseRequest("getProgramInstructions")
                .add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<Map<String, Object>> instructions = mapper.convertValue(
                            response.data.get("instructions"),
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    ObservableList<ExecutionController.InstructionRow> rows = FXCollections.observableArrayList();
                    for (Map<String, Object> instr : instructions) {
                        rows.add(new ExecutionController.InstructionRow(
                                (Integer) instr.get("number"),
                                (String) instr.get("type"),
                                (String) instr.get("arch"),
                                (String) instr.get("label"),
                                (String) instr.get("instruction"),
                                (Integer) instr.get("cycle")
                        ));
                    }

                    instructionTable.setItems(rows);
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
    }

    private void loadInstructionHistory(int index) {
        BaseRequest req = new BaseRequest("getHistoryInstruction")
                .add("username", clientUsername)
                .add("instructionNumber", index);

        sendRequest("http://localhost:8080/api", req, response -> {
            Platform.runLater(() -> {
                if (response.ok) {
                    List<Map<String, Object>> instructions = mapper.convertValue(
                            response.data.get("historyInstruction"),
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    ObservableList<ExecutionController.InstructionBaseRow> rows = FXCollections.observableArrayList();
                    for (Map<String, Object> instr : instructions) {
                        rows.add(new ExecutionController.InstructionRow(
                                (Integer) instr.get("number"),
                                (String) instr.get("type"),
                                (String) instr.get("arch"),
                                (String) instr.get("label"),
                                (String) instr.get("instruction"),
                                (Integer) instr.get("cycle")
                        ));
                    }

                    historyInstrTable.setItems(rows);
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
    }

    private void onFuncsSelection(String selectedFunc) {
        if (selectedFunc == null || selectedFunc.isEmpty()) return;

        historyInstrTable.getItems().clear();
        BaseRequest req = new BaseRequest("setWokFunctionUser")
                .add("username", clientUsername)
                .add("funcName", selectedFunc);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {
                    showStatus(response.message, Alert.AlertType.INFORMATION);
                    loadProgramInstructions();
                    loadHighlightComboBox();
                    loadInputVariables();
                    setRangeDegree(0);
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    private void setRangeDegree(int degree)
    {
        BaseRequest req = new BaseRequest("getDegreeProgram")
                .add("username", clientUsername);

        sendRequest("http://localhost:8080/api", req, response -> {
            if (response.ok) {
                Platform.runLater(() -> {
                    Object val = response.data.get("degree");
                    maxDegree = (Integer) val;
                    expandLabel.setText("Range degrees (0–" + maxDegree + ")");
                    expandField.setText(String.valueOf(degree));
                });
            } else {
                Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
            }
        });
    }

    private TextField createIntegerField() {
        TextField field = new TextField();

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?\\d*")) { // optional minus + digits only
                return change;
            }
            return null; // reject change
        };

        field.setTextFormatter(new TextFormatter<>(filter));
        return field;
    }

    public void createRunParameterFields(List<String> variables) {
        // Remove old paramBox if it exists
        if (paramBox != null) {
            runBox.getChildren().remove(paramBox);
        }
        paramFields.clear();

        if (variables == null || variables.isEmpty())
            return;
        inputVariables.addAll(variables);

        Label paramLabel = new Label("Enter variables");
        paramLabel.setAlignment(Pos.CENTER);

        // Create a new HBox to hold TextFields
        HBox fieldsBox = new HBox(10);
        fieldsBox.setAlignment(Pos.CENTER);



        for (String varName : variables) {
            TextField field = createIntegerField();
            field.setPromptText(varName);
            field.setPrefWidth(80);
            fieldsBox.getChildren().add(field);
            paramFields.add(field);
        }
        paramBox = new VBox(5, paramLabel, fieldsBox);
        paramBox.setAlignment(Pos.CENTER);

        // Insert the paramBox between Label and Button
        int insertIndex = runBox.getChildren().indexOf(runButtonsBox);
        runBox.getChildren().add(insertIndex, paramBox);
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
