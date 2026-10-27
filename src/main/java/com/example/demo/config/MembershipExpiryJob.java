package com.example.demo.config;

import com.example.demo.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Codziennie po północy (i przy starcie aplikacji) oznacza przeterminowane karnety jako wygasłe. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipExpiryJob {

    private final MembershipService membershipService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        expire();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void expire() {
        int expired = membershipService.expireOutdatedMemberships();
        if (expired > 0) {
            log.info("Oznaczono {} karnet(ów) jako wygasłe", expired);
        }
    }
}
