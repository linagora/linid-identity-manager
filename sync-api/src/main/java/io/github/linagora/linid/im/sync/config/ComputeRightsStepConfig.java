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

import io.github.linagora.linid.im.sync.batch.ComputeRightsProcessor;
import io.github.linagora.linid.im.sync.batch.ComputeRightsStepListener;
import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.ComputedRight;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.function.BiFunction;
import org.apache.commons.lang3.Validate;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.support.WindowIterator;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration of the computation of the rights: the readers of the account and application pairs to compute, the
 * writer of their rights, and {@code computeRightsStep}, chaining the reader of the pairs of the cycle, the processor
 * computing the rights and the writer.
 *
 * <p>There is one step-scoped reader per step of the computation: the pairs of the cycle for the global job, the pairs
 * of one account for its recomputation, and the pairs of one application for its recomputation. Each reader gives the
 * pairs whose {@code to_compute} is true one by one, and asks the repository for windows of
 * {@code sync.compute.chunk-size} pairs, each one read by its own short query when the previous one is exhausted: no
 * cursor or transaction stays open across the chunks. The processor and the writer are shared by these steps.</p>
 *
 * <p>Each window starts after the last pair of the previous one, not at an offset: each pair written successfully is
 * no longer {@code to_compute}, and an offset would skip as many pairs. For the same reason, the
 * {@code RepositoryItemReader} of Spring Batch, which reads by page number, is not used. A pair in error stays
 * {@code to_compute}, but is not read again by the same step. The readers keep no state for a restart: a restarted job
 * reads its pairs from the beginning, without the pairs already written.</p>
 */
@Configuration
public class ComputeRightsStepConfig {

    /**
     * Lowest UUID, bound of the account of the first window: PostgreSQL orders it before every other UUID.
     */
    private static final UUID LOWEST_ACCOUNT_ID = new UUID(0L, 0L);

    /**
     * Repository of the account and application pairs.
     */
    private final AccountComputedRightViewRepository repository;

    /**
     * Number of pairs read, computed and written per chunk, also used as the size of the windows of the readers.
     */
    private final int chunkSize;

    /**
     * Creates the configuration of the computation of the rights.
     *
     * @param repository the repository of the account and application pairs
     * @param chunkSize  the number of pairs read, computed and written per chunk
     * @throws IllegalArgumentException when the chunk size is not positive
     */
    public ComputeRightsStepConfig(final AccountComputedRightViewRepository repository,
                                   @Value("${sync.compute.chunk-size}") final int chunkSize) {
        Validate.isTrue(chunkSize > 0, "sync.compute.chunk-size must be positive");
        this.repository = repository;
        this.chunkSize = chunkSize;
    }

    /**
     * Reader of the pairs of the cycle whose rights must be computed, used by the global job.
     *
     * @return the reader of the pairs, ordered by account then application
     */
    @Bean
    @StepScope
    public ItemReader<AccountComputedRightView> cyclePairsReader() {
        return windowReader((position, limit) -> repository
            .findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(accountOf(position), position, limit));
    }

    /**
     * Reader of the pairs of one account whose rights must be computed, used by the recomputation of the account.
     *
     * @param actId the identifier of the account, from the {@code actId} job parameter
     * @return the reader of the pairs of the account, ordered by application
     */
    @Bean
    @StepScope
    public ItemReader<AccountComputedRightView> accountPairsReader(
        @Value("#{jobParameters['actId']}") final UUID actId) {
        return windowReader((position, limit) ->
            repository.findByActIdAndToComputeTrueOrderByAppIdAsc(actId, position, limit));
    }

    /**
     * Reader of the pairs of one application whose rights must be computed, used by the recomputation of the
     * application.
     *
     * @param appId the identifier of the application, from the {@code appId} job parameter
     * @return the reader of the pairs of the application, ordered by account
     */
    @Bean
    @StepScope
    public ItemReader<AccountComputedRightView> applicationPairsReader(
        @Value("#{jobParameters['appId']}") final UUID appId) {
        return windowReader((position, limit) -> repository
            .findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
                appId, accountOf(position), position, limit));
    }

    /**
     * Writer of the rights, saving the rows given by the processor: a new row is persisted, and a changed row, already
     * managed by the persistence context of the chunk, is updated.
     *
     * <p>The writer flushes at the end of each write, as the Spring Batch reference advises for the database writers:
     * an error of an insert or an update is raised by the writer, not at the commit of the chunk. Hibernate still sends
     * the statements of the chunk in groups of {@code hibernate.jdbc.batch_size}.</p>
     *
     * @param entityManagerFactory the {@code EntityManagerFactory} of the linid-api database
     * @return the writer of the rights
     */
    @Bean
    public JpaItemWriter<ComputedRight> computedRightWriter(
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<ComputedRight>()
            .entityManagerFactory(entityManagerFactory)
            .usePersist(true)
            .build();
    }

    /**
     * Step computing and writing the rights of the pairs of the cycle, by chunks of {@code sync.compute.chunk-size}
     * pairs, the last one being smaller. Each chunk is read, processed and written in its own transaction of the
     * linid-api database, whose persistence context is closed at its end.
     *
     * <p>A pair in error is skipped, without limit: it keeps its previous row, if any, stays {@code to_compute} and is
     * computed again at the next cycle. When OPA cannot be reached, the step fails at the first call that cannot reach
     * it: the current chunk is not written, and the chunks already written stay written.</p>
     *
     * @param jobRepository             the repository of the Spring Batch executions
     * @param transactionManager        the transaction manager of the linid-api database
     * @param computeRightsProcessor    the processor computing the rights of the pairs
     * @param computedRightWriter       the writer of the rights
     * @param computeRightsStepListener the listener alerting on the pairs skipped and logging the numbers of pairs
     *     written, in error and discarded
     * @return the step computing and writing the rights of the pairs of the cycle
     */
    @Bean
    public Step computeRightsStep(
        final JobRepository jobRepository,
        final @Qualifier(LinidDataSourceConfig.QUALIFIER) PlatformTransactionManager transactionManager,
        final ComputeRightsProcessor computeRightsProcessor,
        final ItemWriter<ComputedRight> computedRightWriter,
        final ComputeRightsStepListener computeRightsStepListener) {
        return new StepBuilder("computeRightsStep", jobRepository)
            .<AccountComputedRightView, ComputedRight>chunk(chunkSize)
            .transactionManager(transactionManager)
            .reader(cyclePairsReader())
            .processor(computeRightsProcessor)
            .writer(computedRightWriter)
            .listener(computeRightsStepListener)
            .faultTolerant()
            .skip(RightsComputationException.class)
            .skipLimit(Long.MAX_VALUE)
            .build();
    }

    /**
     * Gives the account of a position, which bounds the accounts of the next window.
     *
     * @param position the keyset position of the last pair read, {@link ScrollPosition#keyset()} for the first window
     * @return the account of the position, the lowest UUID for the first window
     */
    private static UUID accountOf(final ScrollPosition position) {
        return (UUID) ((KeysetScrollPosition) position).getKeys().getOrDefault("actId", LOWEST_ACCOUNT_ID);
    }

    /**
     * Builds a reader giving one by one the pairs of the windows returned by a repository method.
     *
     * <p>The first window is read from {@link ScrollPosition#keyset()}, the next ones from the position of the last
     * pair of the previous window, and the reader ends after the last window.</p>
     *
     * @param windows the repository method returning the window of pairs following a position
     * @return the reader of the pairs
     */
    private ItemReader<AccountComputedRightView> windowReader(
        final BiFunction<ScrollPosition, Limit, Window<AccountComputedRightView>> windows) {
        Limit limit = Limit.of(chunkSize);

        return new IteratorItemReader<>(
            WindowIterator.of(position -> windows.apply(position, limit)).startingAt(ScrollPosition.keyset()));
    }
}
