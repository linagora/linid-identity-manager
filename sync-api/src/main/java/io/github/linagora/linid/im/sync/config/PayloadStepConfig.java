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

import io.github.linagora.linid.im.sync.batch.PayloadProcessor;
import io.github.linagora.linid.im.sync.batch.PayloadSourcesReader;
import io.github.linagora.linid.im.sync.batch.PayloadStepListener;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration of the payload build of the accounts: the reader of the accounts and their sources, the writer of the
 * changed payloads, and {@code buildPayloadsStep}, chaining them with the processor building the payloads.
 */
@Configuration
public class PayloadStepConfig {

    /**
     * Repository of {@code account_computed_rights_view}.
     */
    private final AccountComputedRightViewRepository repository;

    /**
     * Repository reading the rows of a source.
     */
    private final SourceRepository sourceRepository;

    /**
     * Configuration holding the sources.
     */
    private final SyncDataProperties properties;

    /**
     * Processor building the payloads.
     */
    private final PayloadProcessor payloadProcessor;

    /**
     * Identifier of the technical account of {@code sync-api}.
     */
    private final UUID technicalAccountId;

    /**
     * Number of accounts read, built and written per chunk, in one transaction.
     */
    private final int chunkSize;

    /**
     * Creates the configuration of the payload build.
     *
     * @param repository         the repository of {@code account_computed_rights_view}
     * @param sourceRepository   the repository reading the rows of a source
     * @param properties         the configuration holding the sources
     * @param payloadProcessor   the processor building the payloads
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @param chunkSize          the number of accounts read, built and written per chunk
     * @throws NullPointerException     when the technical account is not set, since an empty value is bound as
     *     {@code null}, for which the repository would read every account
     * @throws IllegalArgumentException when the chunk size is not positive
     */
    public PayloadStepConfig(final AccountComputedRightViewRepository repository,
                             final SourceRepository sourceRepository,
                             final SyncDataProperties properties,
                             final PayloadProcessor payloadProcessor,
                             @Value("${sync.technical-account-id}") final UUID technicalAccountId,
                             @Value("${sync.payload.chunk-size}") final int chunkSize) {
        Validate.notNull(technicalAccountId, "sync.technical-account-id must be set");
        Validate.isTrue(chunkSize > 0, "sync.payload.chunk-size must be positive");
        this.repository = repository;
        this.sourceRepository = sourceRepository;
        this.properties = properties;
        this.payloadProcessor = payloadProcessor;
        this.technicalAccountId = technicalAccountId;
        this.chunkSize = chunkSize;
    }

    /**
     * Reader of the accounts whose payload is to be built, with their rows in each source: every account of
     * {@code account_computed_rights_view} once, with or without payload, except the technical account of
     * {@code sync-api}, whose payload is never built so that its pairs are never {@code to_compute}.
     *
     * <p>Step-scoped, so that each execution of the step has its own reader. The accounts, with the checksums of
     * their current payloads, and their sources are read by pages of the chunk size. The reader is an
     * {@code ItemStream}, opened, saved and closed by the step, which saves the last account read.</p>
     *
     * @return the reader of the accounts and their sources, ordered by identifier
     */
    @Bean
    @StepScope
    public PayloadSourcesReader payloadSourcesReader() {
        return new PayloadSourcesReader(repository, technicalAccountId, chunkSize, properties, sourceRepository);
    }

    /**
     * Writer of the payloads, saving the rows given by the processor: a new row is persisted, and a changed row,
     * already managed by the persistence context of the chunk, is updated.
     *
     * <p>The writer flushes at the end of each write, as the Spring Batch reference advises for the database writers:
     * an error of an insert or an update is raised by the writer, not at the commit of the chunk. Hibernate still sends
     * the statements of the chunk in groups of {@code hibernate.jdbc.batch_size}.</p>
     *
     * @param entityManagerFactory the {@code EntityManagerFactory} of the linid-api database
     * @return the writer of the payloads
     */
    @Bean
    public JpaItemWriter<AccountPayload> payloadWriter(
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<AccountPayload>()
            .entityManagerFactory(entityManagerFactory)
            .usePersist(true)
            .build();
    }

    /**
     * Step building and writing the payloads of the accounts, by chunks of {@code sync.payload.chunk-size} accounts,
     * the last one being smaller. Each chunk is read, processed and written in its own transaction of the linid-api
     * database, whose persistence context is closed at its end.
     *
     * <p>An account whose payload cannot be built is skipped, without limit: it keeps its previous payload. When the
     * {@code mapping} is invalid, the step fails before reading any account. When a source cannot be read, the step
     * fails: the current chunk is not written, and the chunks already written stay written.</p>
     *
     * @param jobRepository       the repository of the Spring Batch executions
     * @param transactionManager  the transaction manager of the linid-api database
     * @param payloadStepListener the listener checking the {@code mapping}, alerting on the accounts skipped and
     *     logging the outcomes of the accounts
     * @param payloadWriter       the writer of the payloads
     * @return the step building and writing the payloads
     */
    @Bean
    public Step buildPayloadsStep(
        final JobRepository jobRepository,
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) PlatformTransactionManager transactionManager,
        final PayloadStepListener payloadStepListener,
        final ItemWriter<AccountPayload> payloadWriter) {
        return new StepBuilder("buildPayloadsStep", jobRepository)
            .<AccountSnapshot, AccountPayload>chunk(chunkSize)
            .transactionManager(transactionManager)
            .reader(payloadSourcesReader())
            .processor(payloadProcessor)
            .writer(payloadWriter)
            .listener(payloadStepListener)
            .faultTolerant()
            .skip(PayloadException.class)
            .skipLimit(Long.MAX_VALUE)
            .build();
    }

}
