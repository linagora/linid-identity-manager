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

package io.github.linagora.linid.im.sync.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.service.AlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;

@DisplayName("Test class: JobStatusListener")
class JobStatusListenerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private final JobStatusListener listener = new JobStatusListener();

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("afterJob should raise no alert when the job is completed")
    void testAfterJob_completed() {
        listener.afterJob(execution(BatchStatus.COMPLETED));

        assertTrue(appender.list.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = BatchStatus.class, names = {"FAILED", "STOPPED"})
    @DisplayName("afterJob should raise the alert with the job, its status and its parameters when it is not completed")
    void testAfterJob_notCompleted(final BatchStatus status) {
        JobExecution execution = execution(status);

        listener.afterJob(execution);

        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.getFirst().getLevel());
        assertEquals("Job not completed, left to the next cycle — job=accountRecomputeJob, status=" + status
            + ", parameters=" + execution.getJobParameters(), appender.list.getFirst().getFormattedMessage());
        assertNull(appender.list.getFirst().getThrowableProxy());
    }

    @Test
    @DisplayName("afterJob should raise the alert with the failure of the job as cause")
    void testAfterJob_failureException() {
        var failure = new IllegalStateException("job repository unreachable");
        JobExecution execution = execution(BatchStatus.FAILED);
        execution.addFailureException(failure);

        listener.afterJob(execution);

        assertSame(failure, ((ThrowableProxy) appender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    @Test
    @DisplayName("a job whose step fails should raise the alert, its status and its failure being known by afterJob")
    void testAfterJob_failedStep() throws Exception {
        var jobRepository = new ResourcelessJobRepository();
        var failure = new IllegalStateException("step failed");
        var step = new StepBuilder("step", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                throw failure;
            }, new ResourcelessTransactionManager())
            .build();
        var job = new JobBuilder("job", jobRepository).listener(listener).start(step).build();
        var parameters = new JobParameters();
        var instance = jobRepository.createJobInstance(job.getName(), parameters);

        job.execute(jobRepository.createJobExecution(instance, parameters, new ExecutionContext()));

        assertEquals(1, appender.list.size());
        assertTrue(appender.list.getFirst().getFormattedMessage().contains("job=job, status=FAILED"));
        assertSame(failure, ((ThrowableProxy) appender.list.getFirst().getThrowableProxy()).getThrowable());
    }

    private static JobExecution execution(final BatchStatus status) {
        JobParameters parameters = new JobParametersBuilder().addString("accountId", "a1").toJobParameters();
        var execution = new JobExecution(1L, new JobInstance(1L, "accountRecomputeJob"), parameters);
        execution.setStatus(status);
        return execution;
    }
}
