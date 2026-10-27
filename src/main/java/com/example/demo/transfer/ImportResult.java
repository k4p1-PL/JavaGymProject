package com.example.demo.transfer;

import java.util.List;

/** Podsumowanie importu. */
public record ImportResult(
        int plansCreated,
        int plansUpdated,
        int classesCreated,
        int classesSkipped,
        List<String> errors
) {
}
