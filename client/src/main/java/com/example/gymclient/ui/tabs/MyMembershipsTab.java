package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.MembershipDto;
import com.example.gymclient.model.PaymentDto;
import com.example.gymclient.model.PlanDto;
import com.example.gymclient.model.Requests;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Oferta karnetów z zakupem, moje karnety i historia płatności. */
public class MyMembershipsTab extends TabView {

    private final ApiClient api;
    private final TableView<PlanDto> plans = Tables.create();
    private final TableView<MembershipDto> memberships = Tables.create();
    private final TableView<PaymentDto> payments = Tables.create();

    public MyMembershipsTab(ApiClient api) {
        this.api = api;
        Button buy = button("Kup wybrany karnet", this::buy);
        buy.disableProperty().bind(plans.getSelectionModel().selectedItemProperty().isNull());
        refreshButton();

        Tables.column(plans, "Karnet", PlanDto::name);
        Tables.column(plans, "Cena", p -> Fmt.money(p.price()));
        Tables.column(plans, "Okres", p -> p.durationDays() + " dni");
        Tables.column(plans, "Wejścia", p -> p.entryLimit() == null ? "bez limitu" : p.entryLimit());
        Tables.column(plans, "Zajęcia grupowe", p -> p.includesClasses() ? "tak" : "nie");
        Tables.column(plans, "Opis", p -> Fmt.orDash(p.description()));

        Tables.column(memberships, "Karnet", MembershipDto::planName);
        Tables.column(memberships, "Od", m -> Fmt.date(m.startDate()));
        Tables.column(memberships, "Do", m -> Fmt.date(m.endDate()));
        Tables.column(memberships, "Pozostałe wejścia", m -> m.remainingEntries() == null ? "bez limitu" : m.remainingEntries());
        Tables.column(memberships, "Status", m -> Fmt.label(m.status()));
        Tables.column(memberships, "Ważny dziś", m -> m.validToday() ? "✔" : "");

        Tables.column(payments, "Data", p -> Fmt.dateTime(p.paidAt()));
        Tables.column(payments, "Karnet", PaymentDto::planName);
        Tables.column(payments, "Kwota", p -> Fmt.money(p.amount()));
        Tables.column(payments, "Metoda", p -> Fmt.label(p.method()));
        Tables.column(payments, "Status", p -> Fmt.label(p.status()));

        plans.setPrefHeight(200);
        memberships.setPrefHeight(200);
        root.setCenter(new VBox(8, sectionLabel("Oferta"), plans, sectionLabel("Moje karnety"), memberships,
                sectionLabel("Płatności"), payments));
    }

    @Override
    public String title() {
        return "Karnety";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/plans", new TypeReference<List<PlanDto>>() {}), l -> plans.getItems().setAll(l));
        Async.run(() -> api.get("/api/memberships/my", new TypeReference<List<MembershipDto>>() {}), l -> memberships.getItems().setAll(l));
        Async.run(() -> api.get("/api/payments/my", new TypeReference<List<PaymentDto>>() {}), l -> payments.getItems().setAll(l));
    }

    private void buy() {
        PlanDto plan = plans.getSelectionModel().getSelectedItem();
        if (plan == null) return;
        ChoiceBox<String> method = new ChoiceBox<>(FXCollections.observableArrayList("CARD", "ONLINE", "TRANSFER", "CASH"));
        method.setValue("CARD");
        DatePicker start = new DatePicker(java.time.LocalDate.now());
        GridPane grid = Forms.grid();
        Forms.row(grid, "Karnet", new Label(plan.toString()));
        Forms.row(grid, "Płatność", method);
        Forms.row(grid, "Początek", start);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zakup karnetu");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        Async.run(() -> api.post("/api/memberships/purchase",
                new Requests.Purchase(plan.id(), method.getValue(), start.getValue()), MembershipDto.class), m -> {
            Dialogs.info("Kupiono karnet „" + m.planName() + "” ważny do " + Fmt.date(m.endDate()) + ".");
            refresh();
        });
    }
}
