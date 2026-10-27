package com.example.demo.transfer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Karnet w pliku importu/eksportu (JSON i XML). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class PlanData {
    private String name;
    private String description;
    private Integer durationDays;
    private Integer entryLimit;
    private BigDecimal price;
    private boolean includesClasses;
    private boolean active;
}
