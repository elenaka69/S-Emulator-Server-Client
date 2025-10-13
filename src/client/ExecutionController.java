package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import shared.BaseRequest;
import shared.BaseResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ExecutionController {
    private final HttpService http = new HttpService(); // your async HTTP helper
    private final ObjectMapper mapper = new ObjectMapper();
    
    @FXML public Label usernameField;
    @FXML private Label statusBar;

    @FXML private Label expandLabel;
    @FXML private TextField expandField;
    @FXML private Button expandButton;
    @FXML private Button collapseButton;
    @FXML private ComboBox<String> highlightComboBox;
    @FXML private ComboBox<String> funcsComboBox;
    @FXML private TableView<InstructionRow> instructionTable;
    @FXML private TableColumn<InstructionRow, Integer> colNumber;
    @FXML private TableColumn<InstructionRow, String> colType;
    @FXML private TableColumn<InstructionRow, String> colLabel;
    @FXML private TableColumn<InstructionRow, String> colInstruction;
    @FXML private TableColumn<InstructionRow, Integer> colCycle;
    @FXML private TableColumn<InstructionRow, Boolean> colBreakpoint;
    @FXML private TableView<InstructionBaseRow> historyInstrTable;
    @FXML private TableColumn<InstructionBaseRow, Integer> colHistoryNumber;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryType;
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

    private String clientUsername;
    private String programName;
    private int currentHighlightedStep = -1;
    private String highlightText = null;

    public void initialize() {
        setupProgramTable();
        setupHistoryTable();

        instructionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                int index = newSelection.getNumber();
                loadInstructionHistory(index);
            }
        });

        funcsComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            onFuncsSelection(newValue);
        });
    }


    public void startExecutionBoard(String clientUsername, String programName) {
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
        private final String label;
        private final String instruction;
        private final Integer cycle;

        public InstructionBaseRow(int number, String type, String label, String instruction, int cycle) {
            this.number = number;
            this.type = type;
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
    }

    public static class InstructionRow extends InstructionBaseRow{

        private boolean breakpoint = false;
        private String strDataInstruction;

        public InstructionRow(int number, String type, String label, String instruction, int cycle) {
            super(number, type, label, instruction, cycle);
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
    }

    public void onCollapse(ActionEvent actionEvent) {
    }

    public void onStepBackButton(ActionEvent actionEvent) {
    }

    public void onResumeDebug(ActionEvent actionEvent) {
    }

    public void onStepOver(ActionEvent actionEvent) {
    }

    public void onStopDebug(ActionEvent actionEvent) {
    }

    public void onDebug(ActionEvent actionEvent) {
    }

    public void onRun(ActionEvent actionEvent) {
    }

    public void onBackToDashboard(ActionEvent actionEvent) {
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
                        } else if (highlightText != null &&
                                (item.getLabel().contains(highlightText) || item.getInstruction().contains(highlightText))) {
                            setStyle("-fx-background-color: #F08650; -fx-text-fill: black;"); // orange highlight
                        } else {
                            setStyle(""); // default
                        }
                    }
                }
            };
/*
            ContextMenu contextMenu = new ContextMenu();
            MenuItem expandAction = new MenuItem("Expand");
            MenuItem collapseAction = new MenuItem("Collapse");
            expandAction.setOnAction(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    expandSingle(ir.getOp()); // <-- your function
                }
            });
            collapseAction.setOnAction(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    collapseSingle(ir.getOp()); // <-- your function
                }
            });
            contextMenu.getItems().add(expandAction);
            contextMenu.getItems().add(collapseAction);

            // Show context menu only if row is not empty AND type == "S"
            row.setOnContextMenuRequested(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    contextMenu.getItems().clear(); // reset items

                    if ("B".equals(ir.getType())) {
                        contextMenu.getItems().add(collapseAction);
                    } else {
                        contextMenu.getItems().addAll(expandAction, collapseAction);
                    }

                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });
  */
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
        colHistoryLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colHistoryInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colHistoryCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
    }

    private void loadFuncsSelection() {
        BaseRequest req = new BaseRequest("getProgramFunctions")
                .add("program", programName);

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
                        onFuncsSelection(funcList.get(0));
                    }
                } else {
                    Platform.runLater(() -> showStatus(response.message, Alert.AlertType.WARNING));
                }
            });
        });
    }

    private void loadHighlightComboBox() {
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

    private void onFuncsSelection(String newValue) {
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
