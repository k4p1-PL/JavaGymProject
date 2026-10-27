package com.example.demo.transfer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Zajęcia w pliku importu/eksportu. Trener jest wskazany adresem email,
 * a daty są w formacie ISO-8601 (np. 2026-11-03T18:00).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ClassData {
    private String name;
    private String description;
    private String trainerEmail;
    private String startTime;
    private String endTime;
    private Integer capacity;
    private String room;
    private String status;
}
