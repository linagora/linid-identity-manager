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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.batch.ComputeRightsProcessor;
import io.github.linagora.linid.im.sync.batch.ComputeRightsStepListener;
import io.github.linagora.linid.im.sync.exception.OpaUnreachableException;
import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.ComputedRight;
import io.github.linagora.linid.im.sync.persistence.model.PairChecksums;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ApplicationRoleRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ComputedRightRepository;
import io.github.linagora.linid.im.sync.service.RightsComputationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ComputeRightsStepConfig")
class ComputeRightsStepConfigTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    private static final UUID OTHER_APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");

    private static final UUID LOWEST_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final int CHUNK_SIZE = 2;

    private static final Limit LIMIT = Limit.of(CHUNK_SIZE);

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final ComputedRightRecord RIGHTS = new ComputedRightRecord(true, List.of("Administrator"), "{}");

    @Mock
    private AccountComputedRightViewRepository repository;

    @Mock
    private RightsComputationService rightsComputationService;

    @Mock
    private ApplicationRoleRepository applicationRoleRepository;

    @Mock
    private ComputedRightRepository computedRightRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final List<ComputedRight> written = new ArrayList<>();

    private ComputeRightsStepConfig config;

    @BeforeEach
    void setUp() {
        config = new ComputeRightsStepConfig(repository, CHUNK_SIZE);
    }

    @Test
    @DisplayName("constructor should reject a chunk size that is not positive")
    void testConstructor_chunkSizeNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new ComputeRightsStepConfig(repository, 0));
    }

    @Test
    @DisplayName("cyclePairsReader should read the pairs by windows, each one starting after the last pair read and "
        + "bounded by its account")
    void testCyclePairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(ACCOUNT_ID, OTHER_APPLICATION_ID);
        var third = pair(OTHER_ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(true, first, second));
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            ACCOUNT_ID, positionOf(second), LIMIT))
            .thenReturn(window(false, third));

        ItemReader<AccountComputedRightView> reader = config.cyclePairsReader();

        verifyNoInteractions(repository);
        assertEquals(first, reader.read());
        assertEquals(second, reader.read());
        verify(repository, never()).findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            ACCOUNT_ID, positionOf(second), LIMIT);
        assertEquals(third, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("accountPairsReader should read the pairs of the account of the actId job parameter")
    void testAccountPairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByActIdAndToComputeTrueOrderByAppIdAsc(ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(false, first));

        ItemReader<AccountComputedRightView> reader = config.accountPairsReader(ACCOUNT_ID);

        assertEquals(first, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("applicationPairsReader should read the pairs of the application of the appId job parameter, each "
        + "window bounded by the account of the last pair read")
    void testApplicationPairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(OTHER_ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
            APPLICATION_ID, LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(true, first));
        when(repository.findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
            APPLICATION_ID, ACCOUNT_ID, positionOf(first), LIMIT))
            .thenReturn(window(false, second));

        ItemReader<AccountComputedRightView> reader = config.applicationPairsReader(APPLICATION_ID);

        assertEquals(first, reader.read());
        assertEquals(second, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("computedRightWriter should persist the new rows, leave the managed rows to the persistence context, "
        + "and flush at each write")
    void testComputedRightWriter() throws Exception {
        var entityManagerFactory = mock(EntityManagerFactory.class);
        var entityManager = mock(EntityManager.class);
        var newRow = new ComputedRight();
        newRow.setActId(ACCOUNT_ID);
        var managedRow = new ComputedRight();
        managedRow.setActId(OTHER_ACCOUNT_ID);
        when(entityManager.contains(newRow)).thenReturn(false);
        when(entityManager.contains(managedRow)).thenReturn(true);
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            config.computedRightWriter(entityManagerFactory).write(new Chunk<>(newRow, managedRow));
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        }

        var order = inOrder(entityManager);
        order.verify(entityManager).persist(newRow);
        order.verify(entityManager).flush();
        verify(entityManager, never()).persist(managedRow);
        verify(entityManager, never()).merge(any());
    }

    @Test
    @DisplayName("computeRightsStep should read, compute and write the pairs of the cycle by chunks, each one in a "
        + "transaction, skipping the pairs in error and filtering out the pairs changed since they were read")
    void testComputeRightsStep() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(ACCOUNT_ID, OTHER_APPLICATION_ID);
        var third = pair(OTHER_ACCOUNT_ID, APPLICATION_ID);
        cycleWindows(first, second, third);
        when(rightsComputationService.compute(first, Set.of())).thenReturn(RIGHTS);
        when(rightsComputationService.compute(second, Set.of())).thenThrow(new RightsComputationException(
            "Unable to compute the rights of the pair", new IllegalStateException("Unclosed tag")));
        when(rightsComputationService.compute(third, Set.of())).thenReturn(RIGHTS);
        when(repository.findChecksumsByActIdAndAppId(ACCOUNT_ID, APPLICATION_ID))
            .thenReturn(Optional.of(new PairChecksums(ACCOUNT_ID, APPLICATION_ID, null, null, null)));
        when(repository.findChecksumsByActIdAndAppId(OTHER_ACCOUNT_ID, APPLICATION_ID)).thenReturn(Optional.empty());
        when(computedRightRepository.findByActIdAndAppId(ACCOUNT_ID, APPLICATION_ID)).thenReturn(Optional.empty());
        var stepExecution = stepExecution();

        step().execute(stepExecution);

        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
        assertEquals(3, stepExecution.getReadCount());
        assertEquals(1, stepExecution.getWriteCount());
        assertEquals(1, stepExecution.getProcessSkipCount());
        assertEquals(1, stepExecution.getFilterCount());
        assertEquals(1, written.size());
        assertEquals(ACCOUNT_ID, written.getFirst().getActId());
        assertEquals(APPLICATION_ID, written.getFirst().getAppId());
        verify(transactionManager, times(2)).commit(any());
    }

    @Test
    @DisplayName("computeRightsStep should fail at the first pair whose OPA call cannot reach OPA, without writing the "
        + "chunk")
    void testComputeRightsStep_opaUnreachable() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(ACCOUNT_ID, OTHER_APPLICATION_ID);
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(false, first, second));
        when(rightsComputationService.compute(first, Set.of())).thenThrow(new OpaUnreachableException(
            "Unable to reach the OPA server", new ResourceAccessException("Connection refused")));
        var stepExecution = stepExecution();

        step().execute(stepExecution);

        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
        verify(rightsComputationService, never()).compute(second, Set.of());
        assertTrue(written.isEmpty());
        verifyNoInteractions(computedRightRepository);
        verify(transactionManager).rollback(any());
    }

    private Step step() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        ItemWriter<ComputedRight> writer = chunk -> written.addAll(chunk.getItems());
        return config.computeRightsStep(new ResourcelessJobRepository(), transactionManager,
            new ComputeRightsProcessor(rightsComputationService, applicationRoleRepository, repository,
                computedRightRepository, TECHNICAL_ACCOUNT_ID, "sync-api-1"),
            writer, new ComputeRightsStepListener());
    }

    /**
     * Gives the pairs of the cycle by windows of 2 pairs.
     */
    private void cycleWindows(final AccountComputedRightView first, final AccountComputedRightView second,
                              final AccountComputedRightView third) {
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(true, first, second));
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            second.getActId(), positionOf(second), LIMIT))
            .thenReturn(window(false, third));
    }

    private static StepExecution stepExecution() {
        return MetaDataInstanceFactory.createStepExecution(
            MetaDataInstanceFactory.createJobExecution(42L), "computeRightsStep", 7L);
    }

    private static AccountComputedRightView pair(final UUID actId, final UUID appId) {
        var pair = new AccountComputedRightView();
        pair.setActId(actId);
        pair.setAppId(appId);
        pair.setToCompute(true);
        return pair;
    }

    private static Window<AccountComputedRightView> window(final boolean hasNext,
                                                           final AccountComputedRightView... pairs) {
        return Window.from(List.of(pairs), index -> positionOf(pairs[index]), hasNext);
    }

    private static ScrollPosition positionOf(final AccountComputedRightView pair) {
        return ScrollPosition.forward(Map.of("actId", pair.getActId(), "appId", pair.getAppId()));
    }
}
