package com.example.gymclient.ui.tabs;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/** Wspólna baza zakładek: tytuł, pasek przycisków i odświeżanie danych. */
public abstract class TabView {

    protected final BorderPane root = new BorderPane();
    protected final HBox toolbar = new HBox(8);

    protected TabView() {
        root.setPadding(new Insets(12));
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        toolbar.getStyleClass().add("toolbar");
        root.setTop(toolbar);
    }

    public abstract String title();

    /** Pobiera aktualne dane z serwera. */
    public abstract void refresh();

    public Node content() {
        return root;
    }

    protected Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> action.run());
        toolbar.getChildren().add(button);
        return button;
    }

    protected void refreshButton() {
        Button b = button("Odśwież", this::refresh);
        b.getStyleClass().add("secondary");
    }

    protected Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section");
        return label;
    }
}
