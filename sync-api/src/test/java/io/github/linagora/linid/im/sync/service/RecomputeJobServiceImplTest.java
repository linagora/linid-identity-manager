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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.batch.AccountRecomputeJobConfig;
import io.github.linagora.linid.im.sync.model.recompute.RecomputeRightsDTO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: RecomputeJobServiceImpl")
class RecomputeJobServiceImplTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Mock
    private SessionLockService sessionLockService;

    @Mock
    private SessionLock lock;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job accountRecomputeJob;

    private final List<Runnable> tasks = new ArrayList<>();

    private final Logger logger = (Logger) LoggerFactory.getLogger(RecomputeJobServiceImpl.class);

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private ListAppender<ILoggingEvent> appender;

    private ListAppender<ILoggingEvent> alertAppender;

    private RecomputeJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = service(tasks::add);
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
    @DisplayName("recomputeAccountRights should take the lock of the account, start accountRecomputeJob in the "
        + "background with the account and the date of the request as identifying parameters, and close the lock "
        + "once the job ended")
    void testRecomputeAccountRights() throws Exception {
        accountLock(Optional.of(lock));
        var before = LocalDateTime.now(ZoneOffset.UTC);

        assertEquals(new RecomputeRightsDTO(false), service.recomputeAccountRights(ACCOUNT_ID));

        assertEquals(1, tasks.size());
        verifyNoInteractions(jobOperator);
        verify(lock, never()).close();

        tasks.getFirst().run();

        var parameters = ArgumentCaptor.forClass(JobParameters.class);
        var order = inOrder(jobOperator, lock);
        order.verify(jobOperator).start(eq(accountRecomputeJob), parameters.capture());
        order.verify(lock).close();
        JobParameter<?> accountId = parameters.getValue().getParameter(AccountRecomputeJobConfig.ACCOUNT_ID);
        assertNotNull(accountId);
        assertEquals(ACCOUNT_ID, accountId.value());
        assertTrue(accountId.identifying());
        JobParameter<?> requestedAt = parameters.getValue().getParameter(AccountRecomputeJobConfig.REQUESTED_AT);
        assertNotNull(requestedAt);
        assertTrue(requestedAt.identifying());
        var value = (LocalDateTime) requestedAt.value();
        assertFalse(value.isBefore(before) || value.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        assertTrue(appender.list.isEmpty());
    }

    @Test
    @DisplayName("recomputeAccountRights should start no job and log it when a recomputation of the account is already "
        + "running")
    void testRecomputeAccountRights_alreadyRunning() {
        accountLock(Optional.empty());

        assertEquals(new RecomputeRightsDTO(true), service.recomputeAccountRights(ACCOUNT_ID));

        assertTrue(tasks.isEmpty());
        verifyNoInteractions(jobOperator);
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.getFirst().getLevel());
        assertEquals("Recomputation of account " + ACCOUNT_ID + " not started: a recomputation of the account is "
            + "already running", appender.list.getFirst().getFormattedMessage());
    }

    @Test
    @DisplayName("recomputeAccountRights should raise the alert on an exception of the launch of the job and close the "
        + "lock")
    void testRecomputeAccountRights_launchFailure() throws Exception {
        accountLock(Optional.of(lock));
        var exception = new InvalidJobParametersException("The JobParameters do not contain required keys");
        when(jobOperator.start(eq(accountRecomputeJob), any())).thenThrow(exception);
        service.recomputeAccountRights(ACCOUNT_ID);

        tasks.getFirst().run();

        verify(lock).close();
        assertJobNotRun(exception);
    }

    @Test
    @DisplayName("recomputeAccountRights should raise the alert on an unexpected exception of the job and close the "
        + "lock")
    void testRecomputeAccountRights_jobException() throws Exception {
        accountLock(Optional.of(lock));
        var exception = new IllegalStateException("job repository unreachable");
        when(jobOperator.start(eq(accountRecomputeJob), any())).thenThrow(exception);
        service.recomputeAccountRights(ACCOUNT_ID);

        tasks.getFirst().run();

        verify(lock).close();
        assertJobNotRun(exception);
    }

    @Test
    @DisplayName("recomputeAccountRights should close the lock and let the exception through when the executor rejects "
        + "the job")
    void testRecomputeAccountRights_taskRejected() {
        accountLock(Optional.of(lock));
        var exception = new TaskRejectedException("executor shut down");
        service = service(task -> {
            throw exception;
        });

        assertSame(exception,
            assertThrows(TaskRejectedException.class, () -> service.recomputeAccountRights(ACCOUNT_ID)));

        verifyNoInteractions(jobOperator);
        verify(lock).close();
    }

    @Test
    @DisplayName("recomputeAccountRights should close the lock and raise the alert when the executor drops the job "
        + "still waiting at its shutdown")
    void testRecomputeAccountRights_droppedAtShutdown() {
        accountLock(Optional.of(lock));
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.initialize();
        var neverReleased = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                neverReleased.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        service(executor).recomputeAccountRights(ACCOUNT_ID);

        executor.shutdown();

        verifyNoInteractions(jobOperator);
        verify(lock).close();
        assertEquals(1, alertAppender.list.size());
        assertEquals("Job not started, left to the next cycle — job=accountRecomputeJob, account=" + ACCOUNT_ID,
            alertAppender.list.getFirst().getFormattedMessage());
        assertInstanceOf(CancellationException.class,
            ((ThrowableProxy) alertAppender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    @Test
    @DisplayName("recomputeAccountRights should let the exception through when the lock cannot be taken")
    void testRecomputeAccountRights_lockError() {
        var exception = new CannotGetJdbcConnectionException("connection refused");
        when(sessionLockService.tryLock(anyInt(), anyInt())).thenThrow(exception);

        assertSame(exception, assertThrows(CannotGetJdbcConnectionException.class,
            () -> service.recomputeAccountRights(ACCOUNT_ID)));

        assertTrue(tasks.isEmpty());
        verifyNoInteractions(jobOperator);
    }

    private RecomputeJobServiceImpl service(final TaskExecutor taskExecutor) {
        return new RecomputeJobServiceImpl(sessionLockService, taskExecutor, jobOperator, accountRecomputeJob);
    }

    private void assertJobNotRun(final Exception exception) {
        assertTrue(appender.list.isEmpty());
        assertEquals(1, alertAppender.list.size());
        assertEquals(Level.ERROR, alertAppender.list.getFirst().getLevel());
        assertEquals("Job not started, left to the next cycle — job=accountRecomputeJob, account=" + ACCOUNT_ID,
            alertAppender.list.getFirst().getFormattedMessage());
        assertSame(exception, ((ThrowableProxy) alertAppender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    private void accountLock(final Optional<SessionLock> accountLock) {
        when(sessionLockService.tryLock(LockNamespaces.ACCOUNT_RECOMPUTE, ACCOUNT_ID.hashCode()))
            .thenReturn(accountLock);
    }
}
