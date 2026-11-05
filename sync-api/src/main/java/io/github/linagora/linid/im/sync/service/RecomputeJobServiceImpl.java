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

import io.github.linagora.linid.im.sync.batch.AccountRecomputeJobConfig;
import io.github.linagora.linid.im.sync.batch.ApplicationRecomputeJobConfig;
import io.github.linagora.linid.im.sync.model.recompute.RecomputeRightsDTO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link RecomputeJobService}, with session advisory locks of the {@code sync-api} database.
 *
 * <p>A recomputation of an account takes the lock {@code (LockNamespaces.ACCOUNT_RECOMPUTE, key)}, the key being the
 * hash code of its {@code act_id}, and a recomputation of an application the lock
 * {@code (LockNamespaces.APPLICATION_RECOMPUTE, key)}, the key being the hash code of its {@code app_id}, with
 * {@link SessionLockService}, kept during the whole job: an instance that crashes during the job does not block the
 * next recomputations. Two accounts, or two applications, with the same key share the lock: while one is recomputed,
 * the recomputation of the other one is not started, and is reported as already running.</p>
 *
 * <p>The job runs in a task of the application task executor, so that the request is answered without waiting for
 * it. In the task, {@link JobOperator} runs the job synchronously, then the lock is closed, even when the job fails or
 * cannot be launched. A recomputation holds its connection as soon as it waits for a thread:
 * {@code spring.task.execution.pool} limits the tasks running and waiting, and the executor rejects the next ones.</p>
 *
 * <p>When the instance stops, the tasks running and waiting go on during
 * {@code spring.lifecycle.timeout-per-shutdown-phase}. The executor then cancels the tasks still waiting: each one
 * closes its lock and raises the {@code jobNotStarted} alert, since the request is already answered.</p>
 */
@Slf4j
@Service
public class RecomputeJobServiceImpl implements RecomputeJobService {

    /**
     * Service taking the session locks.
     */
    private final SessionLockService sessionLockService;

    /**
     * Executor running the jobs in the background.
     */
    private final TaskExecutor taskExecutor;

    /**
     * Operator starting the jobs.
     */
    private final JobOperator jobOperator;

    /**
     * Job recomputing the rights of one account.
     */
    private final Job accountRecomputeJob;

    /**
     * Job recomputing the rights of all the accounts on one application.
     */
    private final Job applicationRecomputeJob;

    /**
     * Creates the service of the recomputations of the rights.
     *
     * @param sessionLockService      the service taking the session locks
     * @param taskExecutor            the application task executor, running the jobs in the background
     * @param jobOperator             the operator starting the jobs
     * @param accountRecomputeJob     the job recomputing the rights of one account
     * @param applicationRecomputeJob the job recomputing the rights of all the accounts on one application
     */
    public RecomputeJobServiceImpl(
        final SessionLockService sessionLockService,
        @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) final TaskExecutor taskExecutor,
        final JobOperator jobOperator,
        final Job accountRecomputeJob,
        final Job applicationRecomputeJob) {
        this.sessionLockService = sessionLockService;
        this.taskExecutor = taskExecutor;
        this.jobOperator = jobOperator;
        this.accountRecomputeJob = accountRecomputeJob;
        this.applicationRecomputeJob = applicationRecomputeJob;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Takes the lock of the recomputation of the account, then submits the job to the executor, with the account
     * and the date of the request as identifying parameters. When the submission fails, the lock is closed at
     * once.</p>
     *
     * @throws org.springframework.dao.DataAccessException when the {@code sync-api} database cannot be reached or the
     *     lock cannot be requested
     * @throws org.springframework.core.task.TaskRejectedException when the queue of the executor is full
     */
    @Override
    public RecomputeRightsDTO recomputeAccountRights(final UUID accountId) {
        return recompute(LockNamespaces.ACCOUNT_RECOMPUTE, "account", accountId, accountRecomputeJob,
            AccountRecomputeJobConfig.ACCOUNT_ID, AccountRecomputeJobConfig.REQUESTED_AT);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Takes the lock of the recomputation of the application, then submits the job to the executor, with the
     * application and the date of the request as identifying parameters. When the submission fails, the lock is closed
     * at once.</p>
     *
     * @throws org.springframework.dao.DataAccessException when the {@code sync-api} database cannot be reached or the
     *     lock cannot be requested
     * @throws org.springframework.core.task.TaskRejectedException when the queue of the executor is full
     */
    @Override
    public RecomputeRightsDTO recomputeApplicationRights(final UUID applicationId) {
        return recompute(LockNamespaces.APPLICATION_RECOMPUTE, "application", applicationId, applicationRecomputeJob,
            ApplicationRecomputeJobConfig.APPLICATION_ID, ApplicationRecomputeJobConfig.REQUESTED_AT);
    }

    /**
     * Takes the lock of a recomputation, then submits its job to the executor, with the entity and the date of the
     * request as identifying parameters.
     *
     * @param namespace            the namespace of the lock
     * @param target               the kind of entity recomputed, in the logs and the alerts
     * @param id                   the identifier of the entity, whose hash code is the key of the lock
     * @param job                  the job of the recomputation
     * @param idParameter          the name of the job parameter holding the identifier of the entity
     * @param requestedAtParameter the name of the job parameter holding the date of the request
     * @return whether a recomputation of the entity was already running
     */
    private RecomputeRightsDTO recompute(final int namespace, final String target, final UUID id, final Job job,
                                         final String idParameter, final String requestedAtParameter) {
        Optional<SessionLock> lock = sessionLockService.tryLock(namespace, id.hashCode());
        if (lock.isEmpty()) {
            log.info("Recomputation of {} {} not started: a recomputation of the {} is already running", target, id,
                target);
            return new RecomputeRightsDTO(true);
        }
        JobParameters parameters = new JobParametersBuilder()
            .addJobParameter(idParameter, id, UUID.class)
            .addLocalDateTime(requestedAtParameter, LocalDateTime.now(ZoneOffset.UTC))
            .toJobParameters();
        submitJob(lock.get(), job, parameters, "job=%s, %s=%s".formatted(job.getName(), target, id));
        return new RecomputeRightsDTO(false);
    }

    /**
     * Submits the job of a recomputation to the executor, closing its lock when the submission fails or when the
     * executor cancels the task before it starts, the instance stopping.
     *
     * @param lock       the lock of the recomputation
     * @param job        the job of the recomputation
     * @param parameters the parameters of the job
     * @param context    the context of the alert raised when the job is not started
     */
    private void submitJob(final SessionLock lock, final Job job, final JobParameters parameters,
                           final String context) {
        try {
            taskExecutor.execute(new FutureTask<Void>(() -> runJob(lock, job, parameters, context), null) {
                @Override
                protected void done() {
                    if (isCancelled()) {
                        lock.close();
                        AlertService.jobNotStarted(context, new CancellationException("The instance is stopping"));
                    }
                }
            });
        } catch (RuntimeException e) {
            lock.close();
            throw e;
        }
    }

    /**
     * Runs the job of a recomputation, then closes its lock. An exception of the launch raises an alert: the request
     * is already answered.
     *
     * @param lock       the lock of the recomputation
     * @param job        the job of the recomputation
     * @param parameters the parameters of the job
     * @param context    the context of the alert raised when the job is not started
     */
    private void runJob(final SessionLock lock, final Job job, final JobParameters parameters, final String context) {
        try (lock) {
            jobOperator.start(job, parameters);
        } catch (JobExecutionException | RuntimeException e) {
            AlertService.jobNotStarted(context, e);
        }
    }
}
