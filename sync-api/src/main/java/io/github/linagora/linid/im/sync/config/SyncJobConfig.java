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

package io.github.linagora.linid.im.sync.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of {@code syncJob}, the global job of the synchronization cycle, started by
 * {@link io.github.linagora.linid.im.sync.service.SyncScheduler}.
 */
@Configuration
public class SyncJobConfig {

    /**
     * Global job: builds and writes the payloads of all the accounts, then computes the rights of the pairs of the
     * cycle.
     *
     * <p>{@code computeRightsStep} only runs when {@code buildPayloadsStep} succeeds: when it fails, the job stops and
     * ends {@code FAILED}, and the rights are computed at the next cycle, since computing a new script with the
     * previous payloads would store wrong rights.</p>
     *
     * <p>The job is not restartable: each time of the cron is one job instance, and Spring Batch refuses to start a
     * job instance that already exists, so that another instance never runs the cycle of the same time again, nor
     * restarts a failed one.</p>
     *
     * @param jobRepository     the repository of the Spring Batch executions
     * @param buildPayloadsStep the step building and writing the payloads
     * @param computeRightsStep the step computing and writing the rights of the pairs of the cycle
     * @return the global job
     */
    @Bean
    public Job syncJob(final JobRepository jobRepository, final Step buildPayloadsStep, final Step computeRightsStep) {
        return new JobBuilder("syncJob", jobRepository)
            .preventRestart()
            .start(buildPayloadsStep)
            .next(computeRightsStep)
            .build();
    }
}
