package com.example.demo.transfer;

import com.example.demo.exception.BusinessException;
import com.example.demo.model.GymClass;
import com.example.demo.model.MembershipPlan;
import com.example.demo.model.User;
import com.example.demo.model.enums.ClassStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.GymClassRepository;
import com.example.demo.repository.MembershipPlanRepository;
import com.example.demo.repository.UserRepository;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Eksport i import danych siłowni (karnety + grafik zajęć) w formatach JSON i XML.
 * JSON obsługuje Jackson (konwersja w kontrolerze), XML – JAXB.
 */
@Service
@RequiredArgsConstructor
public class DataTransferService {

    private final MembershipPlanRepository planRepository;
    private final GymClassRepository classRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GymDataExport exportData() {
        GymDataExport export = new GymDataExport();
        export.setExportedAt(LocalDateTime.now(clock).withNano(0).toString());
        planRepository.findAllByOrderByPriceAsc().forEach(p -> export.getPlans().add(new PlanData(
                p.getName(), p.getDescription(), p.getDurationDays(), p.getEntryLimit(),
                p.getPrice(), p.isIncludesClasses(), p.isActive())));
        classRepository.findAll().forEach(c -> export.getClasses().add(new ClassData(
                c.getName(), c.getDescription(), c.getTrainer().getEmail(),
                c.getStartTime().toString(), c.getEndTime().toString(),
                c.getCapacity(), c.getRoom(), c.getStatus().name())));
        return export;
    }

    public String toXml(GymDataExport data) {
        try {
            Marshaller marshaller = JAXBContext.newInstance(GymDataExport.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(data, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Nie udało się wygenerować XML", e);
        }
    }

    public GymDataExport fromXml(String xml) {
        try {
            return (GymDataExport) JAXBContext.newInstance(GymDataExport.class)
                    .createUnmarshaller()
                    .unmarshal(new StringReader(xml));
        } catch (JAXBException | ClassCastException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Niepoprawny plik XML: " + rootMessage(e));
        }
    }

    /**
     * Import danych: karnety są dopasowywane po nazwie (aktualizacja albo utworzenie),
     * zajęcia są dodawane, jeśli w grafiku nie ma jeszcze takich samych (nazwa + termin + trener).
     * Błędne pozycje są pomijane i opisane w wyniku.
     */
    @Transactional
    public ImportResult importData(GymDataExport data) {
        if (data == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Pusty plik importu");
        }
        List<String> errors = new ArrayList<>();
        int plansCreated = 0, plansUpdated = 0, classesCreated = 0, classesSkipped = 0;

        List<PlanData> plans = data.getPlans() != null ? data.getPlans() : List.of();
        for (int i = 0; i < plans.size(); i++) {
            PlanData pd = plans.get(i);
            String label = "Karnet #" + (i + 1);
            String error = validatePlan(pd);
            if (error != null) {
                errors.add(label + ": " + error);
                continue;
            }
            Optional<MembershipPlan> existing = planRepository.findByNameIgnoreCase(pd.getName().trim());
            MembershipPlan plan = existing.orElseGet(MembershipPlan::new);
            plan.setName(pd.getName().trim());
            plan.setDescription(pd.getDescription());
            plan.setDurationDays(pd.getDurationDays());
            plan.setEntryLimit(pd.getEntryLimit());
            plan.setPrice(pd.getPrice());
            plan.setIncludesClasses(pd.isIncludesClasses());
            plan.setActive(pd.isActive());
            planRepository.save(plan);
            if (existing.isPresent()) plansUpdated++;
            else plansCreated++;
        }

        List<ClassData> classes = data.getClasses() != null ? data.getClasses() : List.of();
        for (int i = 0; i < classes.size(); i++) {
            ClassData cd = classes.get(i);
            String label = "Zajęcia #" + (i + 1) + (cd.getName() != null ? " (" + cd.getName() + ")" : "");
            try {
                if (cd.getName() == null || cd.getName().isBlank()) throw new IllegalArgumentException("brak nazwy");
                if (cd.getCapacity() == null || cd.getCapacity() < 1) throw new IllegalArgumentException("niepoprawny limit miejsc");
                LocalDateTime start = LocalDateTime.parse(cd.getStartTime());
                LocalDateTime end = LocalDateTime.parse(cd.getEndTime());
                if (!end.isAfter(start)) throw new IllegalArgumentException("koniec przed początkiem");
                User trainer = userRepository.findByEmailIgnoreCase(String.valueOf(cd.getTrainerEmail()).trim())
                        .filter(u -> u.getRole() == Role.TRAINER)
                        .orElseThrow(() -> new IllegalArgumentException("brak trenera " + cd.getTrainerEmail()));
                if (classRepository.existsByNameIgnoreCaseAndStartTimeAndTrainerId(cd.getName().trim(), start, trainer.getId())) {
                    classesSkipped++;
                    continue;
                }
                GymClass gc = GymClass.builder()
                        .name(cd.getName().trim())
                        .description(cd.getDescription())
                        .trainer(trainer)
                        .startTime(start)
                        .endTime(end)
                        .capacity(cd.getCapacity())
                        .room(cd.getRoom())
                        .status(cd.getStatus() != null ? ClassStatus.valueOf(cd.getStatus()) : ClassStatus.SCHEDULED)
                        .build();
                classRepository.save(gc);
                classesCreated++;
            } catch (DateTimeParseException | NullPointerException e) {
                errors.add(label + ": niepoprawna data (oczekiwany format 2026-11-03T18:00)");
            } catch (IllegalArgumentException e) {
                errors.add(label + ": " + e.getMessage());
            }
        }
        return new ImportResult(plansCreated, plansUpdated, classesCreated, classesSkipped, errors);
    }

    private String validatePlan(PlanData pd) {
        if (pd.getName() == null || pd.getName().isBlank()) return "brak nazwy";
        if (pd.getDurationDays() == null || pd.getDurationDays() < 1) return "niepoprawna liczba dni";
        if (pd.getPrice() == null || pd.getPrice().compareTo(BigDecimal.ZERO) < 0) return "niepoprawna cena";
        if (pd.getEntryLimit() != null && pd.getEntryLimit() < 1) return "niepoprawny limit wejść";
        return null;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
