package com.example.gymclient.ui;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.AuthResponse;
import com.example.gymclient.model.Requests;
import com.example.gymclient.util.Async;
import com.example.gymclient.util.Dialogs;
import com.example.gymclient.util.Forms;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Ekran logowania i rejestracji klubowicza. */
public class LoginView {

    private final ApiClient api;
    private final Consumer<AuthResponse> onLoggedIn;
    private final VBox root = new VBox(16);

    public LoginView(ApiClient api, Consumer<AuthResponse> onLoggedIn) {
        this.api = api;
        this.onLoggedIn = onLoggedIn;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(32));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("login");

        Label title = new Label("GymManager");
        title.getStyleClass().add("title");
        Label subtitle = new Label("System do obsługi siłowni");
        subtitle.getStyleClass().add("subtitle");

        TabPane tabs = new TabPane(loginTab(), registerTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Label server = new Label("Serwer: " + api.getBaseUrl());
        server.getStyleClass().add("muted");

        root.getChildren().addAll(title, subtitle, tabs, server);
    }

    private Tab loginTab() {
        TextField email = new TextField();
        email.setPromptText("np. jan@gym.pl");
        PasswordField password = new PasswordField();
        Button login = new Button("Zaloguj");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = Forms.grid();
        Forms.row(grid, "Email", email);
        Forms.row(grid, "Hasło", password);
        grid.add(login, 1, grid.getRowCount());

        Label hint = new Label("Konta demo: admin@gym.pl / admin123, trener@gym.pl / trener123, jan@gym.pl / klient123");
        hint.setWrapText(true);
        hint.getStyleClass().add("muted");

        login.setOnAction(e -> Async.run(() -> api.post("/api/auth/login",
                        new Requests.Login(email.getText().trim(), password.getText()), AuthResponse.class),
                this::loggedIn));
        return new Tab("Logowanie", new VBox(8, grid, hint));
    }

    private Tab registerTab() {
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        TextField firstName = new TextField();
        TextField lastName = new TextField();
        TextField phone = new TextField();
        DatePicker birth = new DatePicker();
        Button register = new Button("Załóż konto");
        register.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = Forms.grid();
        Forms.row(grid, "Email", email);
        Forms.row(grid, "Hasło (min. 6 znaków)", password);
        Forms.row(grid, "Imię", firstName);
        Forms.row(grid, "Nazwisko", lastName);
        Forms.row(grid, "Telefon", phone);
        Forms.row(grid, "Data urodzenia", birth);
        grid.add(register, 1, grid.getRowCount());

        register.setOnAction(e -> Async.run(() -> api.post("/api/auth/register", new Requests.Register(
                        email.getText().trim(), password.getText(), Forms.text(firstName), Forms.text(lastName),
                        Forms.text(phone), birth.getValue()), AuthResponse.class),
                auth -> {
                    Dialogs.info("Konto zostało utworzone. Witaj, " + auth.user().firstName() + "!");
                    loggedIn(auth);
                }));
        return new Tab("Rejestracja", new ScrollPane(grid));
    }

    private void loggedIn(AuthResponse auth) {
        api.setToken(auth.token());
        onLoggedIn.accept(auth);
    }
}
