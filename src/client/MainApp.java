package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // TODO remove load Dashboard Execution fxml. it was for debug only
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Login.fxml"));
        //  FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Dashboard.fxml"));
        //    FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Execution.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("S-Emulator - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
