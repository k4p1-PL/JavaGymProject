package com.example.demo.controller;

import com.example.demo.transfer.DataTransferService;
import com.example.demo.transfer.GymDataExport;
import com.example.demo.transfer.ImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * Import i eksport oferty karnetów oraz grafiku zajęć.
 * Format wybiera parametr ?format=json|xml (eksport) albo nagłówek Content-Type (import).
 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import / eksport", description = "Karnety i grafik zajęć w formacie JSON lub XML (ADMIN)")
public class DataTransferController {

    private final DataTransferService transferService;

    @GetMapping("/export")
    @Operation(summary = "Eksport danych do pliku JSON lub XML")
    public ResponseEntity<?> export(@RequestParam(defaultValue = "json") String format) {
        GymDataExport data = transferService.exportData();
        boolean xml = "xml".equalsIgnoreCase(format);
        String filename = xml ? "gym-data.xml" : "gym-data.json";
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
        if (xml) {
            return response.contentType(new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8))
                    .body(transferService.toXml(data));
        }
        return response.contentType(MediaType.APPLICATION_JSON).body(data);
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Import danych z JSON (treść pliku w ciele żądania)")
    public ImportResult importJson(@RequestBody GymDataExport data) {
        return transferService.importData(data);
    }

    @PostMapping(value = "/import", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    @Operation(summary = "Import danych z XML (treść pliku w ciele żądania)")
    public ImportResult importXml(@RequestBody String xml) {
        return transferService.importData(transferService.fromXml(xml));
    }
}
