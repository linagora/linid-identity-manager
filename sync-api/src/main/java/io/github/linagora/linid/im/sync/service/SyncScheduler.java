/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

package io.github.linagora.linid.im.sync.service;

import io.github.linagora.linid.im.sync.config.SchedulingConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Starts the global job {@code syncJob} at the time of the cron {@code sync.cron}, in UTC, on one instance of
 * {@code sync-api} only.
 *
 * <p>Before starting the job, the instance takes the cycle lock, a session advisory lock of the {@code sync-api}
 * database, on a dedicated connection outside the pool, kept during the whole cycle and closed at its end. Closing it
 * ends the session, and PostgreSQL releases the lock with it: the lock is never left held, and an instance that
 * crashes during the cycle does not block the next ones. When the lock is already held, by another instance or by the
 * previous cycle still running, the cycle is skipped.</p>
 *
 * <p>The cron runs in UTC, so that the changes of the daylight saving time never skip nor repeat a cycle. Each time of
 * the cron is one job instance, identified by this time, and {@code syncJob} is not restartable: an instance that
 * takes the lock once the cycle of the same time ended, its clock being late, does not run it again.</p>
 *
 * <p>The job is started synchronously, Spring Batch running it in the thread of the scheduler: the connection is
 * closed once the job ended.</p>
 */
@Slf4j
@Component
public class SyncScheduler {

    /**
     * Takes the cycle lock without waiting, giving whether it was taken. PostgreSQL releases it when the session ends.
     */
    static final String TRY_LOCK_SQL = "SELECT pg_try_advisory_lock(?, 0)";

    /**
     * Half a second, added to the start date before truncating it to the second, so that the start date is rounded to
     * the nearest second.
     */
    private static final Duration HALF_SECOND = Duration.ofMillis(500);

    /**
     * Datasource of the session locks, opening a new session of the {@code sync-api} database at each connection.
     */
    private final DataSource sessionLockDataSource;

    /**
     * Operator starting the jobs.
     */
    private final JobOperator jobOperator;

    /**
     * Global job of the synchronization cycle.
     */
    private final Job syncJob;

    /**
     * Identifier of the {@code sync-api} instance, written in the logs and in the job parameters.
     */
    private final String instanceId;

    /**
     * Creates the scheduler of the global job.
     *
     * @param sessionLockDataSource the datasource of the session locks, without pool
     * @param jobOperator           the operator starting the jobs
     * @param syncJob               the global job of the synchronization cycle
     * @param instanceId            the identifier of the {@code sync-api} instance
     */
    public SyncScheduler(@Qualifier(SchedulingConfig.SESSION_LOCK_QUALIFIER) final DataSource sessionLockDataSource,
                         final JobOperator jobOperator,
                         final Job syncJob,
                         @Value("${sync.instance-id}") final String instanceId) {
        this.sessionLockDataSource = sessionLockDataSource;
        this.jobOperator = jobOperator;
        this.syncJob = syncJob;
        this.instanceId = instanceId;
    }

    /**
     * Runs a synchronization cycle: takes the cycle lock, starts {@code syncJob} for the time of the cron and closes
     * the connection of the lock, or skips the cycle when the lock is already held.
     *
     * <p>The time of the cron is the start date, in UTC, rounded to the nearest second, read before opening the
     * connection: the scheduler calls the method at the time of the cron, with a gap far below a second, late or
     * slightly early, which Spring's {@code CronTrigger} allows for. Truncated, a start date slightly early would give
     * the previous second, and another job instance.</p>
     */
    @Scheduled(cron = "${sync.cron}", zone = "UTC")
    public void runCycle() {
        LocalDateTime scheduledAt = timeOfCron(LocalDateTime.now(ZoneOffset.UTC));
        Connection connection;
        try {
            connection = sessionLockDataSource.getConnection();
        } catch (SQLException e) {
            log.error("Synchronization cycle of instance {} aborted: the sync-api database cannot be reached",
                instanceId, e);
            return;
        }
        try {
            if (tryLock(connection)) {
                startJob(scheduledAt);
            }
        } finally {
            // Ends the session, which releases the cycle lock: an error on close is only logged at debug level
            JdbcUtils.closeConnection(connection);
        }
    }

    /**
     * Gives the time of the cron of a start date: the start date rounded to the nearest second.
     *
     * @param startDate the start date of the cycle, in UTC
     * @return the time of the cron, in UTC
     */
    static LocalDateTime timeOfCron(final LocalDateTime startDate) {
        return startDate.plus(HALF_SECOND).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Takes the cycle lock on the connection, without waiting, and logs why the cycle is skipped when it is not taken.
     *
     * @param connection the dedicated connection of the cycle
     * @return {@code true} when the lock was taken, {@code false} when it is already held or cannot be requested
     */
    private boolean tryLock(final Connection connection) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(TRY_LOCK_SQL, Boolean.class, LockNamespaces.CYCLE))) {
                return true;
            }
            log.info("Synchronization cycle skipped by instance {}: the cycle lock is held by a running cycle",
                instanceId);
        } catch (DataAccessException e) {
            log.error("Synchronization cycle of instance {} aborted: the cycle lock cannot be taken", instanceId, e);
        }
        return false;
    }

    /**
     * Starts {@code syncJob} for a time of the cron, with this time as identifying parameter, so that each time of the
     * cron is one job instance, and the identifier of the instance as non-identifying parameter.
     *
     * <p>Since {@code syncJob} is not restartable, Spring Batch refuses a time of the cron whose job instance already
     * exists: the cycle of this time was already started, by an instance whose clock is ahead, and is skipped.</p>
     *
     * @param scheduledAt the time of the cron, in UTC
     */
    private void startJob(final LocalDateTime scheduledAt) {
        JobParameters parameters = new JobParametersBuilder()
            .addLocalDateTime("scheduledAt", scheduledAt)
            .addString("instanceId", instanceId, false)
            .toJobParameters();
        try {
            jobOperator.start(syncJob, parameters);
        } catch (JobInstanceAlreadyCompleteException | JobRestartException | JobExecutionAlreadyRunningException e) {
            log.info("Synchronization cycle of {} UTC skipped by instance {}: it was already started", scheduledAt,
                instanceId);
        } catch (JobExecutionException e) {
            log.error("Synchronization cycle not started by instance {}", instanceId, e);
        }
    }
}
