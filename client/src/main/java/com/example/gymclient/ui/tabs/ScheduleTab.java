package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.BookingDto;
import com.example.gymclient.model.GymClassDto;
import com.example.gymclient.model.Requests;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

import java.util.List;

/** Grafik zajęć na najbliższe 14 dni; klubowicz może się zapisać. */
public class ScheduleTab extends TabView {

    private final ApiClient api;
    private final TableView<GymClassDto> table = Tables.create();

    public ScheduleTab(ApiClient api, boolean canBook) {
        this.api = api;
        if (canBook) {
            Button book = button("Zapisz się", this::book);
            book.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        }
        refreshButton();

        Tables.column(table, "Termin", c -> Fmt.dateTime(c.startTime()));
        Tables.column(table, "Do", c -> Fmt.TIME.format(c.endTime()));
        Tables.column(table, "Zajęcia", GymClassDto::name);
        Tables.column(table, "Trener", GymClassDto::trainerName);
        Tables.column(table, "Sala", c -> Fmt.orDash(c.room()));
        Tables.column(table, "Wolne miejsca", c -> c.freeSpots() + " / " + c.capacity());
        Tables.column(table, "Status", c -> Fmt.label(c.status()));
        Tables.column(table, "Opis", c -> Fmt.orDash(c.description()));
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Grafik zajęć";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/classes", new TypeReference<List<GymClassDto>>() {}),
                list -> table.getItems().setAll(list));
    }

    private void book() {
        GymClassDto c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        Async.run(() -> api.post("/api/bookings", new Requests.Booking(c.id()), BookingDto.class), b -> {
            Dialogs.info("Zapisano na zajęcia „" + b.className() + "” (" + Fmt.dateTime(b.classStartTime()) + ").");
            refresh();
        });
    }
}
