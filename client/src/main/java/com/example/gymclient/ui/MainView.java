package com.example.gymclient.ui;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.UserDto;
import com.example.gymclient.ui.tabs.*;
import com.example.gymclient.util.Fmt;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/** Główne okno – zestaw zakładek zależy od roli zalogowanego użytkownika. */
public class MainView {

    private final BorderPane root = new BorderPane();

    public MainView(ApiClient api, UserDto user, Runnable onLogout) {
        Label title = new Label("GymManager");
        title.getStyleClass().add("header-title");
        Label who = new Label(user.fullName() + " · " + Fmt.label(user.role()));
        who.getStyleClass().add("header-user");
        Button logout = new Button("Wyloguj");
        logout.setOnAction(e -> onLogout.run());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, title, spacer, who, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.getStyleClass().add("header");

        List<TabView> views = new ArrayList<>();
        switch (user.role()) {
            case "ADMIN" -> {
                views.add(new StatsTab(api));
                views.add(new ReceptionTab(api));
                views.add(new UsersTab(api, user));
                views.add(new PlansAdminTab(api));
                views.add(new MembershipsAdminTab(api));
                views.add(new ClassesManageTab(api, true));
                views.add(new TransferTab(api));
            }
            case "TRAINER" -> {
                views.add(new ClassesManageTab(api, false));
                views.add(new ScheduleTab(api, false));
            }
            default -> {
                views.add(new ScheduleTab(api, true));
                views.add(new MyBookingsTab(api));
                views.add(new MyMembershipsTab(api));
                views.add(new MyCheckInsTab(api));
            }
        }
        views.add(new ProfileTab(api));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (TabView view : views) {
            Tab tab = new Tab(view.title(), view.content());
            tab.setOnSelectionChanged(e -> {
                if (tab.isSelected()) view.refresh();
            });
            tabs.getTabs().add(tab);
        }
        views.get(0).refresh();

        root.setTop(header);
        root.setCenter(tabs);
    }

    public Parent getRoot() {
        return root;
    }
}
