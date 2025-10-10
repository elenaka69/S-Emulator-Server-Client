package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController {
    @FXML public Label userName;
    @FXML private Label statusBar;

    @FXML public TableView<ConnectedUsersRow> connectedUsersTable;
    @FXML public TableColumn colNumber;
    @FXML public TableColumn colUsername;
    @FXML public TableColumn colLoginTime;
    public TableView<StatisticUserRow>  statisticTable;
    public TableColumn colProperty;
    public TableColumn colValue;

    // Optional: you can store the logged-in username
    private String clientUsername;

    @FXML
    private void onLoadFile() {

    }
    @FXML
    public void onChargeCredits(ActionEvent actionEvent) {
    }

    public void setUsername(String username) {
        this.clientUsername = username;
        userName.setText(username);
        showStatus("Logged in as: " + username, Alert.AlertType.INFORMATION);
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
        private final String propety;
        private final String value;

        public StatisticUserRow(int number, String propety, String value) {
            this.propety = propety;
            this.value = value;
        }
        public String getPropety() {
            return propety;
        }
        public String getValue() {
            return value;
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
