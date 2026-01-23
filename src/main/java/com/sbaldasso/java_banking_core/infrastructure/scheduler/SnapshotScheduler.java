package com.sbaldasso.java_banking_core.infrastructure.scheduler;

import com.sbaldasso.java_banking_core.application.service.SnapshotApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Scheduled job for automated balance snapshot creation.
 * Runs daily at midnight to create snapshots for all active accounts.
 */
@Component
public class SnapshotScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final SnapshotApplicationService snapshotService;

    public SnapshotScheduler(SnapshotApplicationService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * Creates daily snapshots for all accounts at midnight.
     * Cron expression: "0 0 0 * * *" = second minute hour day month weekday
     * Runs every day at 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void createDailySnapshots() {
        logger.info("Starting scheduled snapshot creation job");

        try {
            // Use end of previous day as snapshot time
            Instant snapshotTime = getEndOfPreviousDay();

            int snapshotsCreated = snapshotService.createSnapshotsForAllAccounts(snapshotTime);

            logger.info("Scheduled snapshot creation completed successfully: {} snapshots created",
                    snapshotsCreated);

        } catch (Exception e) {
            logger.error("Scheduled snapshot creation failed: {}", e.getMessage(), e);
            // Note: In production, you might want to send alerts/notifications here
        }
    }

    /**
     * Gets the Instant representing the end of the previous day (23:59:59.999).
     * This ensures snapshots capture the full day's balance.
     */
    private Instant getEndOfPreviousDay() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ZonedDateTime endOfDay = yesterday.atTime(23, 59, 59, 999_999_999)
                .atZone(ZoneId.systemDefault());
        return endOfDay.toInstant();
    }

    /**
     * Manual trigger for on-demand snapshot creation (primarily for testing).
     * Can be called via JMX or other management interfaces.
     */
    public void triggerManualSnapshot() {
        logger.info("Manual snapshot creation triggered");
        createDailySnapshots();
    }
}
