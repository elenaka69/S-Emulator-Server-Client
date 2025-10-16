package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Login.fxml"));

        Scene scene = new Scene(loader.load());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/client/icons/app.png")));
        stage.setTitle("S-Emulator - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
