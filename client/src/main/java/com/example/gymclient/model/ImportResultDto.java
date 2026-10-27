package com.example.gymclient.model;

import java.util.List;

public record ImportResultDto(int plansCreated, int plansUpdated, int classesCreated, int classesSkipped,
                              List<String> errors) {
}
