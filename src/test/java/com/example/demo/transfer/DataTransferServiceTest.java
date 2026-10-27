package com.example.demo.transfer;

import com.example.demo.model.GymClass;
import com.example.demo.model.MembershipPlan;
import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.GymClassRepository;
import com.example.demo.repository.MembershipPlanRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataTransferServiceTest {

    @Mock MembershipPlanRepository planRepository;
    @Mock GymClassRepository classRepository;
    @Mock UserRepository userRepository;

    DataTransferService service;

    @BeforeEach
    void setUp() {
        service = new DataTransferService(planRepository, classRepository, userRepository, Clock.systemDefaultZone());
    }

    @Test
    void xmlRoundTripKeepsAllData() {
        GymDataExport data = new GymDataExport();
        data.setExportedAt("2026-11-02T10:00");
        data.getPlans().add(new PlanData("Open", "Opis z polskimi znakami: żółć", 30, null, new BigDecimal("149.00"), true, true));
        data.getClasses().add(new ClassData("Joga", null, "trener@gym.pl", "2026-11-03T18:00", "2026-11-03T19:00", 12, "Sala 2", "SCHEDULED"));

        String xml = service.toXml(data);
        GymDataExport parsed = service.fromXml(xml);

        assertThat(xml).contains("<gymData>").contains("<plan>").contains("<gymClass>");
        assertThat(parsed.getPlans()).hasSize(1);
        assertThat(parsed.getPlans().get(0).getDescription()).isEqualTo("Opis z polskimi znakami: żółć");
        assertThat(parsed.getPlans().get(0).getPrice()).isEqualByComparingTo("149.00");
        assertThat(parsed.getClasses().get(0).getTrainerEmail()).isEqualTo("trener@gym.pl");
    }

    @Test
    void importCreatesNewPlanAndUpdatesExistingOne() {
        MembershipPlan existing = MembershipPlan.builder().id(1L).name("Open").durationDays(30)
                .price(BigDecimal.TEN).active(true).build();
        when(planRepository.findByNameIgnoreCase("Open")).thenReturn(Optional.of(existing));
        when(planRepository.findByNameIgnoreCase("Nowy")).thenReturn(Optional.empty());

        GymDataExport data = new GymDataExport();
        data.getPlans().add(new PlanData("Open", null, 30, null, new BigDecimal("159.00"), true, true));
        data.getPlans().add(new PlanData("Nowy", null, 14, 5, new BigDecimal("59.00"), false, true));

        ImportResult result = service.importData(data);

        assertThat(result.plansUpdated()).isEqualTo(1);
        assertThat(result.plansCreated()).isEqualTo(1);
        assertThat(existing.getPrice()).isEqualByComparingTo("159.00");
        verify(planRepository, times(2)).save(any(MembershipPlan.class));
    }

    @Test
    void importReportsErrorsAndSkipsInvalidEntries() {
        User trainer = User.builder().id(2L).email("trener@gym.pl").role(Role.TRAINER).build();
        when(userRepository.findByEmailIgnoreCase("trener@gym.pl")).thenReturn(Optional.of(trainer));
        when(userRepository.findByEmailIgnoreCase("nikt@gym.pl")).thenReturn(Optional.empty());

        GymDataExport data = new GymDataExport();
        data.getPlans().add(new PlanData("", null, 30, null, BigDecimal.ONE, true, true));               // brak nazwy
        data.getClasses().add(new ClassData("Joga", null, "trener@gym.pl", "2026-11-03T18:00", "2026-11-03T19:00", 12, null, null));
        data.getClasses().add(new ClassData("Pilates", null, "nikt@gym.pl", "2026-11-03T18:00", "2026-11-03T19:00", 12, null, null));
        data.getClasses().add(new ClassData("Crossfit", null, "trener@gym.pl", "jutro", "pojutrze", 12, null, null));

        ImportResult result = service.importData(data);

        assertThat(result.classesCreated()).isEqualTo(1);
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors()).anyMatch(e -> e.contains("nikt@gym.pl"));
        assertThat(result.errors()).anyMatch(e -> e.contains("niepoprawna data"));
        verify(classRepository, times(1)).save(any(GymClass.class));
        verify(planRepository, never()).save(any());
    }

    @Test
    void exportContainsPlansAndClasses() {
        User trainer = User.builder().id(2L).email("trener@gym.pl").role(Role.TRAINER).firstName("T").lastName("T").build();
        when(planRepository.findAllByOrderByPriceAsc()).thenReturn(List.of(
                MembershipPlan.builder().name("Open").durationDays(30).price(BigDecimal.TEN).build()));
        when(classRepository.findAll()).thenReturn(List.of(GymClass.builder().name("Joga").trainer(trainer)
                .startTime(java.time.LocalDateTime.of(2026, 11, 3, 18, 0))
                .endTime(java.time.LocalDateTime.of(2026, 11, 3, 19, 0)).capacity(10).build()));

        GymDataExport export = service.exportData();

        assertThat(export.getPlans()).extracting(PlanData::getName).containsExactly("Open");
        assertThat(export.getClasses()).extracting(ClassData::getTrainerEmail).containsExactly("trener@gym.pl");
        assertThat(export.getClasses().get(0).getStartTime()).isEqualTo("2026-11-03T18:00");
    }
}
