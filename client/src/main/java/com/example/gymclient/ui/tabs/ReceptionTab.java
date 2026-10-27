package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.CheckInDto;
import com.example.gymclient.model.Requests;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/** Recepcja: rejestracja wejść i wyjść klubowiczów. */
public class ReceptionTab extends TabView {

    private final ApiClient api;
    private final TableView<CheckInDto> table = Tables.create();

    public ReceptionTab(ApiClient api) {
        this.api = api;
        TextField email = new TextField();
        email.setPromptText("email klubowicza");
        email.setPrefWidth(260);
        toolbar.getChildren().addAll(new Label("Wejście:"), email);
        Button in = button("Wpuść", () -> checkIn(email));
        in.setDefaultButton(true);
        Button out = button("Zarejestruj wyjście", this::checkOut);
        out.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        refreshButton();

        Tables.column(table, "Wejście", c -> Fmt.dateTime(c.checkInTime()));
        Tables.column(table, "Wyjście", c -> c.checkOutTime() == null ? "na siłowni" : Fmt.dateTime(c.checkOutTime()));
        Tables.column(table, "Klubowicz", CheckInDto::memberName);
        Tables.column(table, "Karnet", CheckInDto::planName);
        Tables.column(table, "Pozostałe wejścia", c -> c.remainingEntries() == null ? "bez limitu" : c.remainingEntries());
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Recepcja";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/checkins", new TypeReference<List<CheckInDto>>() {}),
                list -> table.getItems().setAll(list));
    }

    private void checkIn(TextField email) {
        String value = Forms.text(email);
        if (value == null) {
            Dialogs.error("Podaj email klubowicza");
            return;
        }
        Async.run(() -> api.post("/api/checkins", new Requests.CheckIn(null, value), CheckInDto.class), c -> {
            email.clear();
            Dialogs.info("Wpuszczono: " + c.memberName() + " (karnet: " + c.planName() + ")");
            refresh();
        });
    }

    private void checkOut() {
        CheckInDto c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        Async.run(() -> api.post("/api/checkins/" + c.id() + "/checkout", null, CheckInDto.class), r -> refresh());
    }
}
