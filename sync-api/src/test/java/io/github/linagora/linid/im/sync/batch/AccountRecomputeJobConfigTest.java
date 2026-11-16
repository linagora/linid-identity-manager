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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;

@DisplayName("Test class: AccountRecomputeJobConfig")
class AccountRecomputeJobConfigTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final ResourcelessJobRepository jobRepository = new ResourcelessJobRepository();

    private final List<String> executedSteps = new ArrayList<>();

    private final JobStatusListener jobStatusListener = mock(JobStatusListener.class);

    @Test
    @DisplayName("accountRecomputeJob should build the payload of the account, then compute its rights")
    void testAccountRecomputeJob() throws Exception {
        JobExecution execution = run(step("accountPayloadStep", false), step("accountComputeRightsStep", false));

        assertEquals("accountRecomputeJob", execution.getJobInstance().getJobName());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(List.of("accountPayloadStep", "accountComputeRightsStep"), executedSteps);
    }

    @Test
    @DisplayName("accountRecomputeJob should end FAILED, without computing the rights and telling its listener, when "
        + "the payload build fails")
    void testAccountRecomputeJob_payloadFailed() throws Exception {
        JobExecution execution = run(step("accountPayloadStep", true), step("accountComputeRightsStep", false));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("accountPayloadStep"), executedSteps);
        verify(jobStatusListener).afterJob(execution);
    }

    @Test
    @DisplayName("accountRecomputeJob should accept the parameters holding accountId and requestedAt, with other keys")
    void testAccountRecomputeJob_validParameters() {
        JobParameters parameters = new JobParametersBuilder()
            .addJobParameter(AccountRecomputeJobConfig.ACCOUNT_ID, ACCOUNT_ID, UUID.class)
            .addLocalDateTime(AccountRecomputeJobConfig.REQUESTED_AT, LocalDateTime.now())
            .addString("instanceId", "sync-api-1", false)
            .toJobParameters();

        assertDoesNotThrow(() -> job().getJobParametersValidator().validate(parameters));
    }

    @Test
    @DisplayName("accountRecomputeJob should reject the parameters without accountId")
    void testAccountRecomputeJob_withoutAccountId() {
        JobParameters parameters = new JobParametersBuilder()
            .addLocalDateTime(AccountRecomputeJobConfig.REQUESTED_AT, LocalDateTime.now())
            .toJobParameters();

        assertThrows(InvalidJobParametersException.class,
            () -> job().getJobParametersValidator().validate(parameters));
    }

    @Test
    @DisplayName("accountRecomputeJob should reject the parameters without requestedAt")
    void testAccountRecomputeJob_withoutRequestedAt() {
        JobParameters parameters = new JobParametersBuilder()
            .addJobParameter(AccountRecomputeJobConfig.ACCOUNT_ID, ACCOUNT_ID, UUID.class)
            .toJobParameters();

        assertThrows(InvalidJobParametersException.class,
            () -> job().getJobParametersValidator().validate(parameters));
    }

    private Job job() {
        return new AccountRecomputeJobConfig().accountRecomputeJob(jobRepository,
            step("accountPayloadStep", false), step("accountComputeRightsStep", false), jobStatusListener);
    }

    private JobExecution run(final Step accountPayloadStep, final Step accountComputeRightsStep) throws Exception {
        Job job = new AccountRecomputeJobConfig().accountRecomputeJob(jobRepository, accountPayloadStep,
            accountComputeRightsStep, jobStatusListener);
        JobParameters parameters = new JobParametersBuilder()
            .addJobParameter(AccountRecomputeJobConfig.ACCOUNT_ID, ACCOUNT_ID, UUID.class)
            .addLocalDateTime(AccountRecomputeJobConfig.REQUESTED_AT, LocalDateTime.now())
            .toJobParameters();
        JobInstance instance = jobRepository.createJobInstance(job.getName(), parameters);
        JobExecution execution = jobRepository.createJobExecution(instance, parameters, new ExecutionContext());
        job.execute(execution);
        return execution;
    }

    private Step step(final String name, final boolean failing) {
        return new StepBuilder(name, jobRepository)
            .tasklet((contribution, chunkContext) -> {
                executedSteps.add(name);
                if (failing) {
                    throw new IllegalStateException(name + " failed");
                }
                return RepeatStatus.FINISHED;
            }, new ResourcelessTransactionManager())
            .build();
    }
}
