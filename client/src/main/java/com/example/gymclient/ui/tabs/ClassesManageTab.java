package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.BookingDto;
import com.example.gymclient.model.GymClassDto;
import com.example.gymclient.model.Requests;
import com.example.gymclient.model.UserDto;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Zarządzanie zajęciami. Trener widzi swoje zajęcia, administrator – grafik wszystkich
 * (z wyborem trenera). Pod tabelą lista uczestników z oznaczaniem obecności.
 */
public class ClassesManageTab extends TabView {

    private final ApiClient api;
    private final boolean adminMode;
    private final TableView<GymClassDto> table = Tables.create();
    private final TableView<BookingDto> participants = Tables.create();

    public ClassesManageTab(ApiClient api, boolean adminMode) {
        this.api = api;
        this.adminMode = adminMode;
        button("Dodaj zajęcia", () -> edit(null));
        Button edit = button("Edytuj", () -> edit(table.getSelectionModel().getSelectedItem()));
        Button cancel = button("Odwołaj", this::cancelClass);
        Button delete = button("Usuń", this::deleteClass);
        for (Button b : List.of(edit, cancel, delete)) {
            b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        }
        refreshButton();

        Tables.column(table, "Termin", c -> Fmt.dateTime(c.startTime()));
        Tables.column(table, "Do", c -> Fmt.TIME.format(c.endTime()));
        Tables.column(table, "Zajęcia", GymClassDto::name);
        if (adminMode) Tables.column(table, "Trener", GymClassDto::trainerName);
        Tables.column(table, "Sala", c -> Fmt.orDash(c.room()));
        Tables.column(table, "Zapisani", c -> c.bookedCount() + " / " + c.capacity());
        Tables.column(table, "Status", c -> Fmt.label(c.status()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> loadParticipants(sel));

        Tables.column(participants, "Klubowicz", BookingDto::memberName);
        Tables.column(participants, "Zapisano", b -> Fmt.dateTime(b.bookedAt()));
        Tables.column(participants, "Status", b -> Fmt.label(b.status()));
        Button present = new Button("Obecny");
        Button absent = new Button("Nieobecny");
        present.setOnAction(e -> attendance(true));
        absent.setOnAction(e -> attendance(false));
        present.disableProperty().bind(participants.getSelectionModel().selectedItemProperty().isNull());
        absent.disableProperty().bind(participants.getSelectionModel().selectedItemProperty().isNull());

        table.setPrefHeight(380);
        participants.setPrefHeight(200);
        root.setCenter(new VBox(8, table, new HBox(8, sectionLabel("Uczestnicy wybranych zajęć"), present, absent), participants));
    }

    @Override
    public String title() {
        return adminMode ? "Zajęcia" : "Moje zajęcia";
    }

    @Override
    public void refresh() {
        if (adminMode) {
            String from = LocalDate.now().minusDays(7).atStartOfDay().toString();
            String to = LocalDate.now().plusDays(60).atStartOfDay().toString();
            Async.run(() -> api.get("/api/classes?from=" + from + "&to=" + to, new TypeReference<List<GymClassDto>>() {}),
                    list -> table.getItems().setAll(list));
        } else {
            Async.run(() -> api.get("/api/classes/my", new TypeReference<List<GymClassDto>>() {}),
                    list -> table.getItems().setAll(list));
        }
        participants.getItems().clear();
    }

    private void loadParticipants(GymClassDto c) {
        if (c == null) {
            participants.getItems().clear();
            return;
        }
        Async.run(() -> api.get("/api/classes/" + c.id() + "/bookings", new TypeReference<List<BookingDto>>() {}),
                list -> participants.getItems().setAll(list));
    }

    private void edit(GymClassDto existing) {
        if (adminMode) {
            Async.run(() -> api.get("/api/users/trainers", new TypeReference<List<UserDto>>() {}),
                    trainers -> showEditDialog(existing, trainers));
        } else {
            showEditDialog(existing, List.of());
        }
    }

    private void showEditDialog(GymClassDto c, List<UserDto> trainers) {
        boolean isNew = c == null;
        TextField name = new TextField(isNew ? "" : c.name());
        TextArea description = new TextArea(isNew ? "" : c.description());
        description.setPrefRowCount(2);
        description.setWrapText(true);
        DatePicker date = new DatePicker(isNew ? LocalDate.now().plusDays(1) : c.startTime().toLocalDate());
        TextField start = new TextField(isNew ? "18:00" : Fmt.TIME.format(c.startTime()));
        TextField end = new TextField(isNew ? "19:00" : Fmt.TIME.format(c.endTime()));
        TextField capacity = new TextField(isNew ? "12" : String.valueOf(c.capacity()));
        TextField room = new TextField(isNew ? "" : c.room());
        ComboBox<UserDto> trainer = new ComboBox<>(FXCollections.observableArrayList(trainers));
        trainer.setMaxWidth(Double.MAX_VALUE);
        if (!isNew) {
            trainers.stream().filter(t -> t.id().equals(c.trainerId())).findFirst().ifPresent(trainer::setValue);
        }

        GridPane grid = Forms.grid();
        Forms.row(grid, "Nazwa", name);
        Forms.row(grid, "Opis", description);
        if (adminMode) Forms.row(grid, "Trener", trainer);
        Forms.row(grid, "Data", date);
        Forms.row(grid, "Początek (GG:MM)", start);
        Forms.row(grid, "Koniec (GG:MM)", end);
        Forms.row(grid, "Liczba miejsc", capacity);
        Forms.row(grid, "Sala", room);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nowe zajęcia" : "Edycja zajęć");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Requests.GymClass body;
        try {
            if (date.getValue() == null) throw new IllegalArgumentException("Wybierz datę zajęć");
            LocalDateTime startTime = date.getValue().atTime(Forms.time(start, "Początek"));
            LocalDateTime endTime = date.getValue().atTime(Forms.time(end, "Koniec"));
            Long trainerId = adminMode && trainer.getValue() != null ? trainer.getValue().id() : null;
            body = new Requests.GymClass(Forms.text(name), description.getText(), trainerId, startTime, endTime,
                    Forms.integer(capacity, "Liczba miejsc"), Forms.text(room));
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
            return;
        }
        if (isNew) {
            Async.run(() -> api.post("/api/classes", body, GymClassDto.class), r -> refresh());
        } else {
            Async.run(() -> api.put("/api/classes/" + c.id(), body, GymClassDto.class), r -> refresh());
        }
    }

    private void cancelClass() {
        GymClassDto c = table.getSelectionModel().getSelectedItem();
        if (c == null || !Dialogs.confirm("Odwołać zajęcia „" + c.name() + "” (" + Fmt.dateTime(c.startTime()) + ")? Wszystkie zapisy zostaną anulowane.")) return;
        Async.run(() -> api.post("/api/classes/" + c.id() + "/cancel", null, GymClassDto.class), r -> refresh());
    }

    private void deleteClass() {
        GymClassDto c = table.getSelectionModel().getSelectedItem();
        if (c == null || !Dialogs.confirm("Usunąć zajęcia „" + c.name() + "”?")) return;
        Async.run(() -> api.delete("/api/classes/" + c.id()), this::refresh);
    }

    private void attendance(boolean attended) {
        BookingDto b = participants.getSelectionModel().getSelectedItem();
        GymClassDto c = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        Async.run(() -> api.post("/api/bookings/" + b.id() + "/attendance?attended=" + attended, null, BookingDto.class),
                r -> loadParticipants(c));
    }
}
