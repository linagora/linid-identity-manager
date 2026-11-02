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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import javax.sql.DataSource;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SyncScheduler")
class SyncSchedulerTest {

    private static final String INSTANCE_ID = "sync-api-1";

    @Mock
    private DataSource sessionLockDataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement tryLockStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData resultSetMetaData;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job syncJob;

    private final Logger logger = (Logger) LoggerFactory.getLogger(SyncScheduler.class);

    private ListAppender<ILoggingEvent> appender;

    private SyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SyncScheduler(sessionLockDataSource, jobOperator, syncJob, INSTANCE_ID);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("runCycle should take the cycle lock, start syncJob with the time of the cron in UTC as identifying "
        + "parameter and the instance as non-identifying parameter, then close the connection, which releases the "
        + "lock")
    void testRunCycle_lockFree() throws Exception {
        cycleLock(true);
        var before = SyncScheduler.timeOfCron(LocalDateTime.now(ZoneOffset.UTC));

        scheduler.runCycle();

        var parameters = ArgumentCaptor.forClass(JobParameters.class);
        var order = inOrder(tryLockStatement, jobOperator, connection);
        order.verify(tryLockStatement).setObject(1, LockNamespaces.CYCLE);
        order.verify(tryLockStatement).executeQuery();
        order.verify(jobOperator).start(eq(syncJob), parameters.capture());
        order.verify(connection).close();
        verify(tryLockStatement).close();

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
    @DisplayName("runCycle should skip the cycle, log it with the instance and close the connection when the cycle lock "
        + "is already held")
    void testRunCycle_lockHeld() throws Exception {
        cycleLock(false);

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        verify(connection).close();
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.getFirst().getLevel());
        assertEquals("Synchronization cycle skipped by instance sync-api-1: the cycle lock is held by a running cycle",
            appender.list.getFirst().getFormattedMessage());
    }

    @Test
    @DisplayName("runCycle should close the connection and let the exception of syncJob through")
    void testRunCycle_jobException() throws Exception {
        cycleLock(true);
        var exception = new IllegalStateException("job repository unreachable");
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);
        doThrow(new SQLException("connection reset")).when(connection).close();

        assertSame(exception, assertThrows(IllegalStateException.class, scheduler::runCycle));

        verify(connection).close();
    }

    @Test
    @DisplayName("runCycle should not log an error when the connection cannot be closed once the cycle ended")
    void testRunCycle_closeError() throws Exception {
        cycleLock(true);
        doThrow(new SQLException("connection reset")).when(connection).close();

        scheduler.runCycle();

        verify(jobOperator).start(eq(syncJob), any());
        assertTrue(appender.list.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("alreadyStarted")
    @DisplayName("runCycle should skip the cycle and log it with the instance when the job instance of the time of the "
        + "cron already exists")
    void testRunCycle_alreadyStarted(final JobExecutionException exception) throws Exception {
        cycleLock(true);
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);

        scheduler.runCycle();

        verify(connection).close();
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.getFirst().getLevel());
        assertTrue(appender.list.getFirst().getFormattedMessage().matches(
            "Synchronization cycle of \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})? UTC skipped by instance sync-api-1: "
                + "it was already started"));
    }

    @Test
    @DisplayName("runCycle should log an error with the instance and close the connection when syncJob cannot be "
        + "started")
    void testRunCycle_jobNotStarted() throws Exception {
        cycleLock(true);
        var exception = new InvalidJobParametersException("invalid parameters");
        when(jobOperator.start(eq(syncJob), any())).thenThrow(exception);

        scheduler.runCycle();

        verify(connection).close();
        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
        assertEquals("Synchronization cycle not started by instance sync-api-1",
            appender.list.getFirst().getFormattedMessage());
        assertSame(exception, ((ThrowableProxy) appender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    @Test
    @DisplayName("runCycle should log an error with the instance, without starting syncJob, when the sync-api database "
        + "cannot be reached")
    void testRunCycle_noConnection() throws Exception {
        when(sessionLockDataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
        assertEquals("Synchronization cycle of instance sync-api-1 aborted: the sync-api database cannot be reached",
            appender.list.getFirst().getFormattedMessage());
    }

    @Test
    @DisplayName("runCycle should log an error with the instance, without starting syncJob, and close the connection "
        + "when the cycle lock cannot be requested")
    void testRunCycle_lockError() throws Exception {
        when(sessionLockDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SyncScheduler.TRY_LOCK_SQL)).thenThrow(new SQLException("statement timeout"));

        scheduler.runCycle();

        verifyNoInteractions(jobOperator);
        verify(connection).close();
        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
        assertEquals("Synchronization cycle of instance sync-api-1 aborted: the cycle lock cannot be taken",
            appender.list.getFirst().getFormattedMessage());
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

    private void cycleLock(final boolean taken) throws SQLException {
        when(sessionLockDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SyncScheduler.TRY_LOCK_SQL)).thenReturn(tryLockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSet.getBoolean(1)).thenReturn(taken);
    }
}
