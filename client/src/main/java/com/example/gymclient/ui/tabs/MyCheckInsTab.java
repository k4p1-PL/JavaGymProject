package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.CheckInDto;
import com.example.gymclient.util.Async;
import com.example.gymclient.util.Fmt;
import com.example.gymclient.util.Tables;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.TableView;

import java.util.List;

public class MyCheckInsTab extends TabView {

    private final ApiClient api;
    private final TableView<CheckInDto> table = Tables.create();

    public MyCheckInsTab(ApiClient api) {
        this.api = api;
        refreshButton();
        Tables.column(table, "Wejście", c -> Fmt.dateTime(c.checkInTime()));
        Tables.column(table, "Wyjście", c -> c.checkOutTime() == null ? "na siłowni" : Fmt.dateTime(c.checkOutTime()));
        Tables.column(table, "Karnet", CheckInDto::planName);
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Moje wejścia";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/checkins/my", new TypeReference<List<CheckInDto>>() {}),
                list -> table.getItems().setAll(list));
    }
}
