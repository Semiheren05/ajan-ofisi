package com.semih.ajanofisi.office;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes events older than {@code app.office.retention} every night, so the table stays small. */
@Component
public class EventCleanup {

    private final OfficeEventService officeEventService;

    public EventCleanup(OfficeEventService officeEventService) {
        this.officeEventService = officeEventService;
    }

    @Scheduled(cron = "${app.office.cleanup-cron:0 30 3 * * *}")
    void purge() {
        officeEventService.purgeExpired();
    }
}
