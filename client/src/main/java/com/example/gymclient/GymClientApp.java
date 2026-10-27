package com.example.gymclient;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.AuthResponse;
import com.example.gymclient.ui.LoginView;
import com.example.gymclient.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class GymClientApp extends Application {

    private Stage stage;
    private ApiClient api;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.api = new ApiClient(System.getProperty("gym.api.url", "http://localhost:8080"));
        stage.setTitle("GymManager");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        api.logout();
        LoginView login = new LoginView(api, this::showMain);
        setScene(new Scene(login.getRoot(), 480, 560));
    }

    private void showMain(AuthResponse auth) {
        MainView main = new MainView(api, auth.user(), this::showLogin);
        setScene(new Scene(main.getRoot(), 1200, 760));
        stage.centerOnScreen();
    }

    private void setScene(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/example/gymclient/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}
