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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;

@DisplayName("Test class: SyncJobConfig")
class SyncJobConfigTest {

    private final ResourcelessJobRepository jobRepository = new ResourcelessJobRepository();

    private final List<String> executedSteps = new ArrayList<>();

    @Test
    @DisplayName("syncJob should build the payloads, then compute the rights")
    void testSyncJob() throws Exception {
        JobExecution execution = run(step("buildPayloadsStep", false), step("computeRightsStep", false));

        assertEquals("syncJob", execution.getJobInstance().getJobName());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(List.of("buildPayloadsStep", "computeRightsStep"), executedSteps);
    }

    @Test
    @DisplayName("syncJob should end FAILED without computing the rights when the payload build fails")
    void testSyncJob_payloadsFailed() throws Exception {
        JobExecution execution = run(step("buildPayloadsStep", true), step("computeRightsStep", false));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("buildPayloadsStep"), executedSteps);
    }

    @Test
    @DisplayName("syncJob should not be restartable, so that the cycle of a time of the cron runs once")
    void testSyncJob_notRestartable() {
        Job job = new SyncJobConfig().syncJob(jobRepository, step("buildPayloadsStep", false),
            step("computeRightsStep", false));

        assertFalse(job.isRestartable());
    }

    private JobExecution run(final Step buildPayloadsStep, final Step computeRightsStep) throws Exception {
        Job job = new SyncJobConfig().syncJob(jobRepository, buildPayloadsStep, computeRightsStep);
        JobParameters parameters = new JobParameters();
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
