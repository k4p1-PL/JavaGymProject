package com.example.gymclient.util;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Function;

/** Pomocnik do budowania tabel z rekordów. */
public final class Tables {

    private Tables() {
    }

    public static <T> TableView<T> create() {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new javafx.scene.control.Label("Brak danych"));
        return table;
    }

    public static <T> TableColumn<T, Object> column(TableView<T> table, String title, Function<T, Object> value) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue() == null ? null : value.apply(c.getValue())));
        table.getColumns().add(column);
        return column;
    }
}
