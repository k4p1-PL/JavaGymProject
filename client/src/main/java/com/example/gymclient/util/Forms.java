package com.example.gymclient.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Budowanie prostych formularzy i parsowanie pól. */
public final class Forms {

    private Forms() {
    }

    public static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(260);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    public static void row(GridPane grid, String label, Node field) {
        int row = grid.getRowCount();
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    public static String text(TextField field) {
        String value = field.getText() == null ? "" : field.getText().trim();
        return value.isEmpty() ? null : value;
    }

    public static Integer integer(TextField field, String name) {
        String value = text(field);
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Pole \"" + name + "\" musi być liczbą całkowitą");
        }
    }

    public static BigDecimal decimal(TextField field, String name) {
        String value = text(field);
        if (value == null) return null;
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Pole \"" + name + "\" musi być liczbą (np. 149.99)");
        }
    }

    public static LocalTime time(TextField field, String name) {
        String value = text(field);
        if (value == null) throw new IllegalArgumentException("Podaj godzinę w polu \"" + name + "\"");
        try {
            return LocalTime.parse(value.length() == 4 ? "0" + value : value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Pole \"" + name + "\" musi mieć format GG:MM, np. 18:30");
        }
    }
}
