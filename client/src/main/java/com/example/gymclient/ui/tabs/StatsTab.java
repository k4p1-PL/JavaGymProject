package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.StatsDto;
import com.example.gymclient.util.Async;
import com.example.gymclient.util.Fmt;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Panel administratora z podsumowaniem. */
public class StatsTab extends TabView {

    private final ApiClient api;
    private final FlowPane tiles = new FlowPane(12, 12);

    public StatsTab(ApiClient api) {
        this.api = api;
        refreshButton();
        root.setCenter(tiles);
    }

    @Override
    public String title() {
        return "Panel";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/stats", StatsDto.class), s -> tiles.getChildren().setAll(
                tile("Aktywni klubowicze", String.valueOf(s.activeMembers())),
                tile("Trenerzy", String.valueOf(s.activeTrainers())),
                tile("Aktywne karnety", String.valueOf(s.activeMemberships())),
                tile("Nadchodzące zajęcia", String.valueOf(s.upcomingClasses())),
                tile("Wejścia dzisiaj", String.valueOf(s.checkInsToday())),
                tile("Teraz na siłowni", String.valueOf(s.currentlyInGym())),
                tile("Przychód w tym miesiącu", Fmt.money(s.revenueThisMonth()))));
    }

    private VBox tile(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("tile-value");
        Label l = new Label(label);
        l.getStyleClass().add("tile-label");
        VBox box = new VBox(4, v, l);
        box.getStyleClass().add("tile");
        return box;
    }
}
