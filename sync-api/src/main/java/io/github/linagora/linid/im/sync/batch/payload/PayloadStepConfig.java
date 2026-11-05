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

package io.github.linagora.linid.im.sync.batch.payload;

import io.github.linagora.linid.im.sync.batch.AccountRecomputeJobConfig;
import io.github.linagora.linid.im.sync.config.LinidDataSourceConfig;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration of the payload build of the accounts: the readers of the account snapshots, the writer of the changed
 * payloads, and the steps chaining them with the processor building the payloads:
 * {@code allAccountsPayloadStep}, for every account, used by the global job, and {@code accountPayloadStep}, for one
 * account, used by its recomputation.
 */
@Configuration
public class PayloadStepConfig {

    /**
     * Repository of {@code account_computed_rights_view}.
     */
    private final AccountComputedRightViewRepository repository;

    /**
     * Loader of the account snapshots, shared by the readers.
     */
    private final AccountSnapshotLoader loader;

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
     * @param loader             the loader of the account snapshots, shared by the readers
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @param chunkSize          the number of accounts read, built and written per chunk
     * @throws NullPointerException     when the technical account is not set, since an empty value is bound as
     *     {@code null}, for which the repository would read every account
     * @throws IllegalArgumentException when the chunk size is not positive
     */
    public PayloadStepConfig(final AccountComputedRightViewRepository repository,
                             final AccountSnapshotLoader loader,
                             @Value("${sync.technical-account-id}") final UUID technicalAccountId,
                             @Value("${sync.payload.chunk-size}") final int chunkSize) {
        Validate.notNull(technicalAccountId, "sync.technical-account-id must be set");
        Validate.isTrue(chunkSize > 0, "sync.payload.chunk-size must be positive");
        this.repository = repository;
        this.loader = loader;
        this.technicalAccountId = technicalAccountId;
        this.chunkSize = chunkSize;
    }

    /**
     * Reader of the snapshots of the accounts whose payload is to be built: every account of
     * {@code account_computed_rights_view} once, with or without payload, except the technical account of
     * {@code sync-api}, whose payload is never built so that its pairs are never {@code to_compute}.
     *
     * <p>Step-scoped, so that each execution of the step has its own reader. The accounts are read by pages of the
     * chunk size, the accounts of a page being locked before the checksums of their payloads and their rows in each
     * source are read. The reader is an {@code ItemStream}, opened and closed by the step, without state saved for a
     * restart.</p>
     *
     * @return the reader of the snapshots of every account, ordered by identifier
     */
    @Bean
    @StepScope
    public AllAccountSnapshotsReader allAccountSnapshotsReader() {
        return new AllAccountSnapshotsReader(repository, technicalAccountId, chunkSize, loader);
    }

    /**
     * Reader of the snapshot of the account whose payload is recomputed, loaded at its first read, in the transaction
     * of the chunk: none when the account has no row in {@code account_computed_rights_view}, since no application is
     * deployed, nor for the technical account of {@code sync-api}, without any query.
     *
     * <p>Step-scoped, to read the {@code accountId} job parameter.</p>
     *
     * @param accountId the identifier of the account, from the {@code accountId} job parameter
     * @return the reader of the snapshot of the account
     */
    @Bean
    @StepScope
    public AccountSnapshotReader accountSnapshotReader(
        @Value("#{jobParameters['" + AccountRecomputeJobConfig.ACCOUNT_ID + "']}") final UUID accountId) {
        return new AccountSnapshotReader(accountId, technicalAccountId, repository, loader);
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
     * @param payloadProcessor    the processor building the payloads
     * @param payloadStepListener the listener checking the {@code mapping}, alerting on the accounts skipped and
     *     logging the outcomes of the accounts
     * @param payloadWriter       the writer of the payloads
     * @return the step building and writing the payloads
     */
    @Bean
    public Step allAccountsPayloadStep(
        final JobRepository jobRepository,
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) PlatformTransactionManager transactionManager,
        final PayloadProcessor payloadProcessor,
        final PayloadStepListener payloadStepListener,
        final ItemWriter<AccountPayload> payloadWriter) {
        return payloadStep("allAccountsPayloadStep", jobRepository, transactionManager, allAccountSnapshotsReader(),
            payloadProcessor, payloadStepListener, payloadWriter);
    }

    /**
     * Step building and writing the payload of one account, for its recomputation, built like
     * {@code allAccountsPayloadStep}, with the same processor, writer and listener, on the reader of the account.
     *
     * <p>The account is locked, its sources read and its payload written in one transaction of the linid-api
     * database. An account whose payload cannot be built is skipped: it keeps its previous payload. When the
     * {@code mapping} is invalid or a source cannot be read, the step fails, and the job with it.</p>
     *
     * @param jobRepository         the repository of the Spring Batch executions
     * @param transactionManager    the transaction manager of the linid-api database
     * @param accountSnapshotReader the reader of the snapshot of the account, step-scoped
     * @param payloadProcessor      the processor building the payloads
     * @param payloadStepListener   the listener checking the {@code mapping}, alerting on the account skipped and
     *     logging its outcome
     * @param payloadWriter         the writer of the payloads
     * @return the step building and writing the payload of the account
     */
    @Bean
    public Step accountPayloadStep(
        final JobRepository jobRepository,
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) PlatformTransactionManager transactionManager,
        final @Qualifier("accountSnapshotReader") ItemReader<AccountSnapshot> accountSnapshotReader,
        final PayloadProcessor payloadProcessor,
        final PayloadStepListener payloadStepListener,
        final ItemWriter<AccountPayload> payloadWriter) {
        return payloadStep("accountPayloadStep", jobRepository, transactionManager, accountSnapshotReader,
            payloadProcessor, payloadStepListener, payloadWriter);
    }

    /**
     * Builds a step building and writing the payloads of the accounts of a reader, by chunks of
     * {@code sync.payload.chunk-size} accounts, each one read, processed and written in its own transaction, the
     * accounts in error being skipped without limit.
     *
     * @param name                the name of the step
     * @param jobRepository       the repository of the Spring Batch executions
     * @param transactionManager  the transaction manager of the linid-api database
     * @param reader              the reader of the account snapshots
     * @param payloadProcessor    the processor building the payloads
     * @param payloadStepListener the listener checking the {@code mapping}, alerting on the accounts skipped and
     *     logging the outcomes of the accounts
     * @param payloadWriter       the writer of the payloads
     * @return the step building and writing the payloads
     */
    private Step payloadStep(final String name,
                             final JobRepository jobRepository,
                             final PlatformTransactionManager transactionManager,
                             final ItemReader<AccountSnapshot> reader,
                             final PayloadProcessor payloadProcessor,
                             final PayloadStepListener payloadStepListener,
                             final ItemWriter<AccountPayload> payloadWriter) {
        return new StepBuilder(name, jobRepository)
            .<AccountSnapshot, AccountPayload>chunk(chunkSize)
            .transactionManager(transactionManager)
            .reader(reader)
            .processor(payloadProcessor)
            .writer(payloadWriter)
            .listener(payloadStepListener)
            .faultTolerant()
            .skip(PayloadException.class)
            .skipLimit(Long.MAX_VALUE)
            .build();
    }
}
