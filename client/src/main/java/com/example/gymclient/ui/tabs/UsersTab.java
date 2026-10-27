package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.Requests;
import com.example.gymclient.model.UserDto;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/** Zarządzanie kontami (CRUD) – tylko administrator. */
public class UsersTab extends TabView {

    private final ApiClient api;
    private final UserDto currentUser;
    private final TableView<UserDto> table = Tables.create();
    private final ChoiceBox<String> roleFilter = new ChoiceBox<>(FXCollections.observableArrayList("Wszyscy", "MEMBER", "TRAINER", "ADMIN"));

    public UsersTab(ApiClient api, UserDto currentUser) {
        this.api = api;
        this.currentUser = currentUser;
        button("Dodaj", () -> edit(null));
        Button edit = button("Edytuj", () -> edit(table.getSelectionModel().getSelectedItem()));
        Button deactivate = button("Dezaktywuj", this::deactivate);
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deactivate.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        roleFilter.setValue("Wszyscy");
        roleFilter.setOnAction(e -> refresh());
        toolbar.getChildren().addAll(new Label("Rola:"), roleFilter);
        refreshButton();

        Tables.column(table, "Nazwisko i imię", u -> u.lastName() + " " + u.firstName());
        Tables.column(table, "Email", UserDto::email);
        Tables.column(table, "Telefon", u -> Fmt.orDash(u.phoneNumber()));
        Tables.column(table, "Rola", u -> Fmt.label(u.role()));
        Tables.column(table, "Aktywny", u -> u.active() ? "tak" : "nie");
        Tables.column(table, "Utworzony", u -> Fmt.dateTime(u.createdAt()));
        table.setRowFactory(tv -> {
            TableRow<UserDto> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) edit(row.getItem());
            });
            return row;
        });
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Użytkownicy";
    }

    @Override
    public void refresh() {
        String role = roleFilter.getValue();
        String path = "Wszyscy".equals(role) ? "/api/users" : "/api/users?role=" + role;
        Async.run(() -> api.get(path, new TypeReference<List<UserDto>>() {}), list -> table.getItems().setAll(list));
    }

    private void edit(UserDto user) {
        boolean isNew = user == null;
        TextField email = new TextField(isNew ? "" : user.email());
        email.setDisable(!isNew);
        PasswordField password = new PasswordField();
        password.setPromptText(isNew ? "min. 6 znaków" : "puste = bez zmiany");
        TextField firstName = new TextField(isNew ? "" : user.firstName());
        TextField lastName = new TextField(isNew ? "" : user.lastName());
        TextField phone = new TextField(isNew ? "" : user.phoneNumber());
        DatePicker birth = new DatePicker(isNew ? null : user.dateOfBirth());
        ChoiceBox<String> role = new ChoiceBox<>(FXCollections.observableArrayList("MEMBER", "TRAINER", "ADMIN"));
        role.setValue(isNew ? "MEMBER" : user.role());
        CheckBox active = new CheckBox("Konto aktywne");
        active.setSelected(isNew || user.active());
        active.setDisable(isNew);

        GridPane grid = Forms.grid();
        Forms.row(grid, "Email", email);
        Forms.row(grid, "Hasło", password);
        Forms.row(grid, "Imię", firstName);
        Forms.row(grid, "Nazwisko", lastName);
        Forms.row(grid, "Telefon", phone);
        Forms.row(grid, "Data urodzenia", birth);
        Forms.row(grid, "Rola", role);
        Forms.row(grid, "", active);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nowy użytkownik" : "Edycja: " + user.fullName());
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String newPassword = password.getText().isBlank() ? null : password.getText();
        if (isNew) {
            Async.run(() -> api.post("/api/users", new Requests.UserCreate(Forms.text(email), newPassword,
                    Forms.text(firstName), Forms.text(lastName), Forms.text(phone), birth.getValue(), role.getValue()),
                    UserDto.class), u -> refresh());
        } else {
            Async.run(() -> api.put("/api/users/" + user.id(), new Requests.UserUpdate(Forms.text(firstName),
                    Forms.text(lastName), Forms.text(phone), birth.getValue(), role.getValue(), active.isSelected(),
                    newPassword), UserDto.class), u -> refresh());
        }
    }

    private void deactivate() {
        UserDto u = table.getSelectionModel().getSelectedItem();
        if (u == null) return;
        if (u.id().equals(currentUser.id())) {
            Dialogs.error("Nie możesz dezaktywować własnego konta");
            return;
        }
        if (!Dialogs.confirm("Dezaktywować konto " + u.fullName() + "? Historia karnetów i wejść zostanie zachowana.")) return;
        Async.run(() -> api.delete("/api/users/" + u.id()), this::refresh);
    }
}
