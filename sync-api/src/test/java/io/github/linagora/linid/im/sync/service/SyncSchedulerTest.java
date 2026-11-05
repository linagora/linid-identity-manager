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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SyncScheduler")
class SyncSchedulerTest {

    private static final String INSTANCE_ID = "sync-api-1";

    @Mock
    private SessionLockService sessionLockService;

    @Mock
    private SessionLock lock;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job syncJob;

    private final Logger logger = (Logger) LoggerFactory.getLogger(SyncScheduler.class);

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private ListAppender<ILoggingEvent> appender;

    private ListAppender<ILoggingEvent> alertAppender;

    private SyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SyncScheduler(sessionLockService, jobOperator, syncJob, INSTANCE_ID);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("runCycle should take the cycle lock, start syncJob with the time of the cron in UTC as identifying "
        + "parameter and the instance as non-identifying parameter, then close the lock")
    void testRunCycle_lockFree() throws Exception {
        cycleLock(Optional.of(lock));
        var before = SyncScheduler.timeOfCron(LocalDateTime.now(ZoneOffset.UTC));

        scheduler.runCycle();

        var parameters = ArgumentCaptor.forClass(JobParameters.class);
        var order = inOrder(sessionLockService, jobOperator, lock);
        order.verify(sessionLockService).tryLock(LockNamespaces.CYCLE, 0);
        order.verify(jobOperator).start(eq(syncJob), parameters.capture());
        order.verify(lock).close();

        JobParameter<?> scheduledAt = parameters.getValue().getParameter("scheduledAt");
        assertNotNull(scheduledAt);
        assertTrue(scheduledAt.identifying());
        var value = (LocalDateTime) scheduledAt.value();
        assertEquals(0, value.getNano());
        assertFalse(value.isBefore(before)
            || value.isAfter(SyncScheduler.timeOfCron(LocalDateTime.now(ZoneOffset.UTC))));
        JobParameter<?> instanceId = parameters.getValue().getParameter("instanceId");
        assertNotNull(instanceId);
        assertEquals(INSTANCE_ID, instanceId.value());
        assertFalse(instanceId.identifying());
        assertTrue(appender.list.isEmpty());
    }

    @Test
    @DisplayName("runCycle should skip the cycle and log it with the instance when the cycle lock is already held")
    void testRunCycle_lockHeld() {
        cycleLock(Optional.empty());

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.getFirst().getLevel());
        assertEquals("Synchronization cycle skipped by instance sync-api-1: the cycle lock is held by a running cycle",
            appender.list.getFirst().getFormattedMessage());
    }

    @Test
    @DisplayName("runCycle should raise the alert with the instance and close the lock on an unexpected exception of "
        + "syncJob")
    void testRunCycle_jobException() throws Exception {
        cycleLock(Optional.of(lock));
        var exception = new IllegalStateException("job repository unreachable");
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);

        scheduler.runCycle();

        verify(lock).close();
        assertCycleNotStarted("the job cannot be launched", exception);
    }

    @ParameterizedTest
    @MethodSource("alreadyStarted")
    @DisplayName("runCycle should skip the cycle and log it with the instance when the job instance of the time of the "
        + "cron already exists")
    void testRunCycle_alreadyStarted(final JobExecutionException exception) throws Exception {
        cycleLock(Optional.of(lock));
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);

        scheduler.runCycle();

        verify(lock).close();
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.getFirst().getLevel());
        assertTrue(appender.list.getFirst().getFormattedMessage().matches(
            "Synchronization cycle of \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})? UTC skipped by instance sync-api-1: "
                + "it was already started"));
    }

    @Test
    @DisplayName("runCycle should raise the alert with the instance and close the lock when syncJob cannot be started")
    void testRunCycle_jobNotStarted() throws Exception {
        cycleLock(Optional.of(lock));
        var exception = new InvalidJobParametersException("invalid parameters");
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);

        scheduler.runCycle();

        verify(lock).close();
        assertCycleNotStarted("the job cannot be launched", exception);
    }

    @Test
    @DisplayName("runCycle should raise the alert with the instance, without starting syncJob, when the sync-api "
        + "database cannot be reached")
    void testRunCycle_noConnection() {
        var exception = new CannotGetJdbcConnectionException("connection refused");
        when(sessionLockService.tryLock(LockNamespaces.CYCLE, 0)).thenThrow(exception);

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        assertCycleNotStarted("the sync-api database cannot be reached", exception);
    }

    @Test
    @DisplayName("runCycle should raise the alert with the instance, without starting syncJob, when the cycle lock "
        + "cannot be requested")
    void testRunCycle_lockError() {
        var exception = new QueryTimeoutException("statement timeout");
        when(sessionLockService.tryLock(LockNamespaces.CYCLE, 0)).thenThrow(exception);

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        assertCycleNotStarted("the cycle lock cannot be taken", exception);
    }

    @ParameterizedTest
    @MethodSource("startDates")
    @DisplayName("timeOfCron should round the start date to the nearest second, so that a call slightly early keeps the "
        + "time of the cron")
    void testTimeOfCron(final LocalDateTime startDate, final LocalDateTime expected) {
        assertEquals(expected, SyncScheduler.timeOfCron(startDate));
    }

    private static Stream<Arguments> startDates() {
        LocalDateTime cron = LocalDateTime.of(2026, 11, 3, 2, 0);
        return Stream.of(
            Arguments.of(cron.minusNanos(5_000_000), cron),
            Arguments.of(cron, cron),
            Arguments.of(cron.plusNanos(499_999_999), cron),
            Arguments.of(cron.plusNanos(500_000_000), cron.plusSeconds(1)));
    }

    private static Stream<JobExecutionException> alreadyStarted() {
        return Stream.of(
            new JobInstanceAlreadyCompleteException("completed by another instance"),
            new JobRestartException("failed on another instance"),
            new JobExecutionAlreadyRunningException("left running by a crashed instance"));
    }

    private void assertCycleNotStarted(final String reason, final Exception exception) {
        assertTrue(appender.list.isEmpty());
        assertEquals(1, alertAppender.list.size());
        assertEquals(Level.ERROR, alertAppender.list.getFirst().getLevel());
        assertEquals("Job not started, left to the next cycle — job=syncJob, instance=sync-api-1, reason=" + reason,
            alertAppender.list.getFirst().getFormattedMessage());
        assertSame(exception, ((ThrowableProxy) alertAppender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    private void cycleLock(final Optional<SessionLock> cycleLock) {
        when(sessionLockService.tryLock(LockNamespaces.CYCLE, 0)).thenReturn(cycleLock);
    }
}
