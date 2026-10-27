package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.*;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Karnety klubowiczów: sprzedaż na recepcji, anulowanie i lista płatności. */
public class MembershipsAdminTab extends TabView {

    private final ApiClient api;
    private final TableView<MembershipDto> memberships = Tables.create();
    private final TableView<PaymentDto> payments = Tables.create();

    public MembershipsAdminTab(ApiClient api) {
        this.api = api;
        button("Sprzedaj karnet", this::sell);
        Button cancel = button("Anuluj karnet", this::cancel);
        cancel.disableProperty().bind(memberships.getSelectionModel().selectedItemProperty().isNull());
        refreshButton();

        Tables.column(memberships, "Klubowicz", MembershipDto::memberName);
        Tables.column(memberships, "Email", MembershipDto::memberEmail);
        Tables.column(memberships, "Karnet", MembershipDto::planName);
        Tables.column(memberships, "Od", m -> Fmt.date(m.startDate()));
        Tables.column(memberships, "Do", m -> Fmt.date(m.endDate()));
        Tables.column(memberships, "Wejścia", m -> m.remainingEntries() == null ? "bez limitu" : m.remainingEntries());
        Tables.column(memberships, "Status", m -> Fmt.label(m.status()));

        Tables.column(payments, "Data", p -> Fmt.dateTime(p.paidAt()));
        Tables.column(payments, "Klubowicz", PaymentDto::memberName);
        Tables.column(payments, "Karnet", PaymentDto::planName);
        Tables.column(payments, "Kwota", p -> Fmt.money(p.amount()));
        Tables.column(payments, "Metoda", p -> Fmt.label(p.method()));
        Tables.column(payments, "Status", p -> Fmt.label(p.status()));

        memberships.setPrefHeight(320);
        root.setCenter(new VBox(8, sectionLabel("Karnety"), memberships, sectionLabel("Płatności"), payments));
    }

    @Override
    public String title() {
        return "Karnety klubowiczów";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/memberships", new TypeReference<List<MembershipDto>>() {}), l -> memberships.getItems().setAll(l));
        Async.run(() -> api.get("/api/payments", new TypeReference<List<PaymentDto>>() {}), l -> payments.getItems().setAll(l));
    }

    private void sell() {
        Async.run(() -> new Object[]{
                api.get("/api/users?role=MEMBER", new TypeReference<List<UserDto>>() {}),
                api.get("/api/plans", new TypeReference<List<PlanDto>>() {})
        }, data -> {
            @SuppressWarnings("unchecked") List<UserDto> members = ((List<UserDto>) data[0]).stream().filter(UserDto::active).toList();
            @SuppressWarnings("unchecked") List<PlanDto> plans = (List<PlanDto>) data[1];
            showSellDialog(members, plans);
        });
    }

    private void showSellDialog(List<UserDto> members, List<PlanDto> plans) {
        ComboBox<UserDto> member = new ComboBox<>(FXCollections.observableArrayList(members));
        member.setMaxWidth(Double.MAX_VALUE);
        ComboBox<PlanDto> plan = new ComboBox<>(FXCollections.observableArrayList(plans));
        plan.setMaxWidth(Double.MAX_VALUE);
        ChoiceBox<String> method = new ChoiceBox<>(FXCollections.observableArrayList("CASH", "CARD", "TRANSFER"));
        method.setValue("CASH");
        DatePicker start = new DatePicker(LocalDate.now());

        GridPane grid = Forms.grid();
        Forms.row(grid, "Klubowicz", member);
        Forms.row(grid, "Karnet", plan);
        Forms.row(grid, "Płatność", method);
        Forms.row(grid, "Początek", start);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sprzedaż karnetu");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        if (member.getValue() == null || plan.getValue() == null) {
            Dialogs.error("Wybierz klubowicza i karnet");
            return;
        }
        Async.run(() -> api.post("/api/memberships", new Requests.Sell(member.getValue().id(), plan.getValue().id(),
                method.getValue(), start.getValue()), MembershipDto.class), m -> {
            Dialogs.info("Sprzedano karnet „" + m.planName() + "” dla " + m.memberName() + ".");
            refresh();
        });
    }

    private void cancel() {
        MembershipDto m = memberships.getSelectionModel().getSelectedItem();
        if (m == null || !Dialogs.confirm("Anulować karnet „" + m.planName() + "” (" + m.memberName() + ")? Płatność zostanie oznaczona jako zwrócona.")) return;
        Async.run(() -> api.post("/api/memberships/" + m.id() + "/cancel", null, MembershipDto.class), r -> refresh());
    }
}
