package com.example.gymclient.ui.tabs;

import com.example.gymclient.api.ApiClient;
import com.example.gymclient.model.ImportResultDto;
import com.example.gymclient.util.Async;
import com.example.gymclient.util.Dialogs;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Import i eksport oferty karnetów oraz grafiku zajęć (JSON / XML). */
public class TransferTab extends TabView {

    private final ApiClient api;
    private final TextArea log = new TextArea();

    public TransferTab(ApiClient api) {
        this.api = api;
        button("Eksportuj do JSON", () -> export("json"));
        button("Eksportuj do XML", () -> export("xml"));
        button("Importuj plik…", this::importFile);
        log.setEditable(false);
        log.setWrapText(true);
        Label help = new Label("Eksport zapisuje ofertę karnetów i grafik zajęć do pliku. Import przyjmuje plik w tym samym "
                + "formacie: karnety są dopasowywane po nazwie (aktualizacja), zajęcia dodawane, jeśli jeszcze ich nie ma.");
        help.setWrapText(true);
        root.setCenter(new javafx.scene.layout.VBox(8, help, sectionLabel("Dziennik"), log));
    }

    @Override
    public String title() {
        return "Import / eksport";
    }

    @Override
    public void refresh() {
        // brak danych do odświeżania
    }

    private Window window() {
        return root.getScene() != null ? root.getScene().getWindow() : null;
    }

    private void export(String format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz eksport");
        chooser.setInitialFileName("gym-data." + format);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toUpperCase(), "*." + format));
        File file = chooser.showSaveDialog(window());
        if (file == null) return;
        Async.run(() -> {
            String content = api.getText("/api/transfer/export?format=" + format);
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            return file;
        }, f -> log.appendText("Zapisano eksport: " + f.getAbsolutePath() + "\n"));
    }

    private void importFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz plik do importu");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON lub XML", "*.json", "*.xml"),
                new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*"));
        File file = chooser.showOpenDialog(window());
        if (file == null) return;
        Async.run(() -> {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            boolean xml = file.getName().toLowerCase().endsWith(".xml") || content.stripLeading().startsWith("<");
            return api.postText("/api/transfer/import", content, xml ? "application/xml" : "application/json",
                    ImportResultDto.class);
        }, r -> {
            log.appendText("Import " + file.getName() + ": karnety nowe " + r.plansCreated() + ", zaktualizowane "
                    + r.plansUpdated() + "; zajęcia dodane " + r.classesCreated() + ", pominięte (już istnieją) "
                    + r.classesSkipped() + "\n");
            r.errors().forEach(e -> log.appendText("  ! " + e + "\n"));
            if (!r.errors().isEmpty()) {
                Dialogs.info("Import zakończony z " + r.errors().size() + " błędami – szczegóły w dzienniku.");
            }
        });
    }
}
