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

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of {@code applicationRecomputeJob}, the recomputation of the rights of all the accounts on one
 * application, started by {@link io.github.linagora.linid.im.sync.service.RecomputeJobService}.
 */
@Configuration
public class ApplicationRecomputeJobConfig {

    /**
     * Job parameter holding the {@code app_id} of the application to recompute.
     */
    public static final String APPLICATION_ID = "applicationId";

    /**
     * Job parameter holding the date of the request, so that each request is a new job instance.
     */
    public static final String REQUESTED_AT = "requestedAt";

    /**
     * Recomputation of the rights of all the accounts on one application: builds and writes the payloads of all the
     * accounts, then computes the rights on the application of the accounts whose pair changed. The job stops at the
     * first failed step: computing the rights with the previous payloads would store wrong rights.
     *
     * <p>A failed job is not restarted: a new request is a new job instance, which builds the payloads again from the
     * first account and only computes the pairs still {@code to_compute}.</p>
     *
     * <p>The parameters are validated before any job execution is created: {@value #APPLICATION_ID} and
     * {@value #REQUESTED_AT} are required, without optional keys, since optional keys would reject any other key.
     * Without {@value #APPLICATION_ID}, the job would rebuild the payloads of all the accounts, then end
     * {@code COMPLETED} without computing anything.</p>
     *
     * @param jobRepository                the repository of the Spring Batch executions
     * @param allAccountsPayloadStep       the step building and writing the payloads of all the accounts
     * @param applicationComputeRightsStep the step computing and writing the rights of the pairs of the application
     * @param jobStatusListener            the listener raising the alert of a job that did not complete
     * @return the job recomputing the rights of all the accounts on one application
     */
    @Bean
    public Job applicationRecomputeJob(final JobRepository jobRepository,
                                       final Step allAccountsPayloadStep,
                                       final Step applicationComputeRightsStep,
                                       final JobStatusListener jobStatusListener) {
        return new JobBuilder("applicationRecomputeJob", jobRepository)
            .listener(jobStatusListener)
            .validator(new DefaultJobParametersValidator(new String[] {APPLICATION_ID, REQUESTED_AT}, new String[0]))
            .start(allAccountsPayloadStep)
            .next(applicationComputeRightsStep)
            .build();
    }
}
