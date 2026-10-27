package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.BookingDto;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

import java.util.List;

public class MyBookingsTab extends TabView {

    private final ApiClient api;
    private final TableView<BookingDto> table = Tables.create();

    public MyBookingsTab(ApiClient api) {
        this.api = api;
        Button cancel = button("Anuluj rezerwację", this::cancel);
        cancel.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        refreshButton();

        Tables.column(table, "Termin", b -> Fmt.dateTime(b.classStartTime()));
        Tables.column(table, "Zajęcia", BookingDto::className);
        Tables.column(table, "Trener", BookingDto::trainerName);
        Tables.column(table, "Sala", b -> Fmt.orDash(b.room()));
        Tables.column(table, "Status", b -> Fmt.label(b.status()));
        Tables.column(table, "Zapisano", b -> Fmt.dateTime(b.bookedAt()));
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Moje rezerwacje";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/bookings/my", new TypeReference<List<BookingDto>>() {}),
                list -> table.getItems().setAll(list));
    }

    private void cancel() {
        BookingDto b = table.getSelectionModel().getSelectedItem();
        if (b == null || !Dialogs.confirm("Anulować zapis na „" + b.className() + "”?")) return;
        Async.run(() -> api.post("/api/bookings/" + b.id() + "/cancel", null, BookingDto.class), r -> refresh());
    }
}
