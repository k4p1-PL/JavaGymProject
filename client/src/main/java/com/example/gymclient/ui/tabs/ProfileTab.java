package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.Requests;
import com.example.gymclient.model.UserDto;
import com.example.gymclient.util.Async;
import com.example.gymclient.util.Dialogs;
import com.example.gymclient.util.Forms;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ProfileTab extends TabView {

    private final ApiClient api;
    private final Label email = new Label();
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField phone = new TextField();
    private final DatePicker birth = new DatePicker();

    public ProfileTab(ApiClient api) {
        this.api = api;
        toolbar.setVisible(false);
        toolbar.setManaged(false);

        GridPane profile = Forms.grid();
        Forms.row(profile, "Email", email);
        Forms.row(profile, "Imię", firstName);
        Forms.row(profile, "Nazwisko", lastName);
        Forms.row(profile, "Telefon", phone);
        Forms.row(profile, "Data urodzenia", birth);
        Button save = new Button("Zapisz profil");
        save.setOnAction(e -> Async.run(() -> api.put("/api/users/me", new Requests.Profile(
                        Forms.text(firstName), Forms.text(lastName), Forms.text(phone), birth.getValue()), UserDto.class),
                u -> Dialogs.info("Zapisano zmiany.")));
        profile.add(save, 1, profile.getRowCount());

        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        GridPane password = Forms.grid();
        Forms.row(password, "Obecne hasło", current);
        Forms.row(password, "Nowe hasło", next);
        Button change = new Button("Zmień hasło");
        change.setOnAction(e -> Async.run(() -> api.post("/api/users/me/password",
                        new Requests.ChangePassword(current.getText(), next.getText())),
                () -> {
                    current.clear();
                    next.clear();
                    Dialogs.info("Hasło zostało zmienione.");
                }));
        password.add(change, 1, password.getRowCount());

        VBox box = new VBox(8, sectionLabel("Mój profil"), profile, sectionLabel("Zmiana hasła"), password);
        box.setMaxWidth(560);
        root.setCenter(new ScrollPane(box));
    }

    @Override
    public String title() {
        return "Profil";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/users/me", UserDto.class), u -> {
            email.setText(u.email());
            firstName.setText(u.firstName());
            lastName.setText(u.lastName());
            phone.setText(u.phoneNumber());
            birth.setValue(u.dateOfBirth());
        });
    }
}
