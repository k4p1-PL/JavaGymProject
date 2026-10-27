package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.PlanDto;
import com.example.gymclient.model.Requests;
import com.example.gymclient.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/** Oferta karnetów – pełny CRUD dla administratora. */
public class PlansAdminTab extends TabView {

    private final ApiClient api;
    private final TableView<PlanDto> table = Tables.create();

    public PlansAdminTab(ApiClient api) {
        this.api = api;
        button("Dodaj", () -> edit(null));
        Button edit = button("Edytuj", () -> edit(table.getSelectionModel().getSelectedItem()));
        Button delete = button("Usuń", this::delete);
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        refreshButton();

        Tables.column(table, "Nazwa", PlanDto::name);
        Tables.column(table, "Cena", p -> Fmt.money(p.price()));
        Tables.column(table, "Dni", PlanDto::durationDays);
        Tables.column(table, "Limit wejść", p -> p.entryLimit() == null ? "bez limitu" : p.entryLimit());
        Tables.column(table, "Zajęcia", p -> p.includesClasses() ? "tak" : "nie");
        Tables.column(table, "W sprzedaży", p -> p.active() ? "tak" : "nie");
        Tables.column(table, "Opis", p -> Fmt.orDash(p.description()));
        root.setCenter(table);
    }

    @Override
    public String title() {
        return "Oferta karnetów";
    }

    @Override
    public void refresh() {
        Async.run(() -> api.get("/api/plans?onlyActive=false", new TypeReference<List<PlanDto>>() {}),
                list -> table.getItems().setAll(list));
    }

    private void edit(PlanDto plan) {
        boolean isNew = plan == null;
        TextField name = new TextField(isNew ? "" : plan.name());
        TextArea description = new TextArea(isNew ? "" : plan.description());
        description.setPrefRowCount(3);
        description.setWrapText(true);
        TextField days = new TextField(isNew ? "30" : String.valueOf(plan.durationDays()));
        TextField entries = new TextField(isNew || plan.entryLimit() == null ? "" : String.valueOf(plan.entryLimit()));
        entries.setPromptText("puste = bez limitu");
        TextField price = new TextField(isNew ? "" : plan.price().toPlainString());
        CheckBox classes = new CheckBox("Obejmuje zajęcia grupowe");
        classes.setSelected(isNew || plan.includesClasses());
        CheckBox active = new CheckBox("W sprzedaży");
        active.setSelected(isNew || plan.active());

        GridPane grid = Forms.grid();
        Forms.row(grid, "Nazwa", name);
        Forms.row(grid, "Opis", description);
        Forms.row(grid, "Liczba dni", days);
        Forms.row(grid, "Limit wejść", entries);
        Forms.row(grid, "Cena (zł)", price);
        Forms.row(grid, "", classes);
        Forms.row(grid, "", active);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nowy karnet" : "Edycja karnetu");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Requests.Plan body;
        try {
            body = new Requests.Plan(Forms.text(name), description.getText(), Forms.integer(days, "Liczba dni"),
                    Forms.integer(entries, "Limit wejść"), Forms.decimal(price, "Cena"),
                    classes.isSelected(), active.isSelected());
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
            return;
        }
        if (isNew) {
            Async.run(() -> api.post("/api/plans", body, PlanDto.class), p -> refresh());
        } else {
            Async.run(() -> api.put("/api/plans/" + plan.id(), body, PlanDto.class), p -> refresh());
        }
    }

    private void delete() {
        PlanDto p = table.getSelectionModel().getSelectedItem();
        if (p == null || !Dialogs.confirm("Usunąć karnet „" + p.name() + "”?")) return;
        Async.run(() -> api.delete("/api/plans/" + p.id()), this::refresh);
    }
}
