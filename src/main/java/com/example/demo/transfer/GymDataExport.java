package com.example.demo.transfer;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Główny obiekt pliku importu/eksportu: oferta karnetów i grafik zajęć. */
@Data
@NoArgsConstructor
@XmlRootElement(name = "gymData")
@XmlAccessorType(XmlAccessType.FIELD)
public class GymDataExport {

    private String exportedAt;

    @XmlElementWrapper(name = "plans")
    @XmlElement(name = "plan")
    private List<PlanData> plans = new ArrayList<>();

    @XmlElementWrapper(name = "classes")
    @XmlElement(name = "gymClass")
    private List<ClassData> classes = new ArrayList<>();
}
