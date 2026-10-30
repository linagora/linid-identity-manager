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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.batch.PayloadProcessor;
import io.github.linagora.linid.im.sync.batch.PayloadStepListener;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.model.payload.PayloadOutcome;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.ChecksumService;
import io.github.linagora.linid.im.sync.service.ChecksumServiceImpl;
import io.github.linagora.linid.im.sync.service.JinjaServiceImpl;
import io.github.linagora.linid.im.sync.service.PayloadServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.data.domain.Limit;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: PayloadStepConfig")
class PayloadStepConfigTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final String MAPPING = "{\"email\": {{ accounts.email | tojson }}}";

    private static final ChecksumService CHECKSUM_SERVICE = new ChecksumServiceImpl();

    private static final AccountSummary JDOE =
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe", null);

    private static final AccountSummary ASMITH = new AccountSummary(
        UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith",
        CHECKSUM_SERVICE.compute("{\"email\":\"asmith@example.com\"}"));

    private static final AccountSummary BLEE =
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000003"), "blee", null);

    private final AccountComputedRightViewRepository repository = mock(AccountComputedRightViewRepository.class);

    private final SourceRepository sourceRepository = mock(SourceRepository.class);

    private final AccountPayloadRepository payloadRepository = mock(AccountPayloadRepository.class);

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    private final StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

    private final List<AccountPayload> written = new ArrayList<>();

    private final ItemWriter<AccountPayload> writer = chunk -> written.addAll(chunk.getItems());

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        page(new UUID(0L, 0L), JDOE, ASMITH);
        page(ASMITH.actId(), BLEE);
        when(repository.existsByActId(any())).thenReturn(true);
    }

    @Test
    @DisplayName("payloadSourcesReader should read every account once, except the technical account, in order, by "
        + "pages of the chunk size, with its rows in each source")
    void testPayloadSourcesReader() throws Exception {
        page(ASMITH.actId());
        rows(List.of(JDOE, ASMITH), row(JDOE, "jdoe@example.com"));

        var reader = config(MAPPING, 2).payloadSourcesReader();

        assertEquals(new AccountSnapshot(JDOE, Map.of("accounts", List.of(row(JDOE, "jdoe@example.com")))),
            reader.read());
        assertEquals(new AccountSnapshot(ASMITH, Map.of("accounts", List.of())), reader.read());
        assertNull(reader.read());
    }

    @Test
    @DisplayName("payloadWriter should persist the new rows, leave the managed rows to the persistence context, and "
        + "flush at each write")
    void testPayloadWriter() throws Exception {
        var entityManagerFactory = mock(EntityManagerFactory.class);
        var entityManager = mock(EntityManager.class);
        var newRow = newRow(JDOE, "jdoe@example.com");
        var managedRow = newRow(ASMITH, "asmith@example.com");
        when(entityManager.contains(managedRow)).thenReturn(true);
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            config(MAPPING, 2).payloadWriter(entityManagerFactory).write(new Chunk<>(newRow, managedRow));
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
    @DisplayName("buildPayloadsStep should read, process and write the accounts by chunks, each one in a transaction, "
        + "filtering out the unchanged payloads and skipping the accounts in error")
    void testBuildPayloadsStep() throws Exception {
        rows(List.of(JDOE, ASMITH), row(JDOE, "jdoe@example.com"), row(ASMITH, "asmith@example.com"));
        rows(List.of(BLEE), row(BLEE, "blee@example.com"), row(BLEE, "bruce@example.com"));

        step(MAPPING, 2).execute(stepExecution);

        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
        assertEquals(3, stepExecution.getReadCount());
        assertEquals(1, stepExecution.getWriteCount());
        assertEquals(1, stepExecution.getFilterCount());
        assertEquals(1, stepExecution.getProcessSkipCount());
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.IN_ERROR.key()));
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.INSERTED.key()));
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.UNCHANGED.key()));
        assertEquals(List.of(newRow(JDOE, "jdoe@example.com")), written);
        verify(transactionManager, times(2)).commit(any());
    }

    @Test
    @DisplayName("buildPayloadsStep should fail before reading any source when the mapping is invalid")
    void testBuildPayloadsStep_invalidMapping() throws Exception {
        step("{\"email\": {{ accounts.email }}}", 2).execute(stepExecution);

        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
        verifyNoInteractions(sourceRepository, payloadRepository);
        assertTrue(written.isEmpty());
    }

    @Test
    @DisplayName("buildPayloadsStep should fail when a source cannot be read, without writing the chunk")
    void testBuildPayloadsStep_unreadableSource() throws Exception {
        when(sourceRepository.findAllByAccountIds(anyString(), anyString(), anyCollection()))
            .thenThrow(new IllegalStateException("relation does not exist"));

        step(MAPPING, 2).execute(stepExecution);

        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
        assertTrue(written.isEmpty());
        verify(transactionManager).rollback(any());
    }

    @Test
    @DisplayName("buildPayloadsStep should fail when a chunk cannot be written, without counting its accounts")
    void testBuildPayloadsStep_writeError() throws Exception {
        rows(List.of(JDOE, ASMITH), row(JDOE, "jdoe@example.com"), row(ASMITH, "asmith@example.com"));
        rows(List.of(BLEE), row(BLEE, "blee@example.com"));
        ItemWriter<AccountPayload> failingWriter = chunk -> {
            if (chunk.getItems().getFirst().getActId().equals(BLEE.actId())) {
                throw new IllegalStateException("duplicate key value violates unique constraint");
            }
        };

        config(MAPPING, 2).buildPayloadsStep(new ResourcelessJobRepository(), transactionManager,
            new PayloadStepListener(payloadService(MAPPING)), failingWriter).execute(stepExecution);

        assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.INSERTED.key()));
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.UNCHANGED.key()));
        verify(transactionManager).commit(any());
        verify(transactionManager).rollback(any());
    }

    @Test
    @DisplayName("constructor should reject a technical account that is not set, before any read")
    void testConstructor_technicalAccountNotSet() {
        var exception = assertThrows(NullPointerException.class, () -> new PayloadStepConfig(repository,
            sourceRepository, properties(MAPPING), processor(MAPPING), null, 2));

        assertEquals("sync.technical-account-id must be set", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("constructor should reject a chunk size that is not positive")
    void testConstructor_chunkSizeNotPositive() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new PayloadStepConfig(repository,
            sourceRepository, properties(MAPPING), processor(MAPPING), TECHNICAL_ACCOUNT_ID, 0));

        assertEquals("sync.payload.chunk-size must be positive", exception.getMessage());
    }

    private PayloadStepConfig config(final String mapping, final int chunkSize) {
        return new PayloadStepConfig(repository, sourceRepository, properties(mapping), processor(mapping),
            TECHNICAL_ACCOUNT_ID, chunkSize);
    }

    /**
     * Gives the accounts of the page following the given account, by pages of 2 accounts.
     */
    private void page(final UUID after, final AccountSummary... accounts) {
        when(repository.findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(TECHNICAL_ACCOUNT_ID, after,
            Limit.of(2))).thenReturn(List.of(accounts));
    }

    private Step step(final String mapping, final int chunkSize) {
        return config(mapping, chunkSize).buildPayloadsStep(new ResourcelessJobRepository(), transactionManager,
            new PayloadStepListener(payloadService(mapping)), writer);
    }

    private static SyncDataProperties properties(final String mapping) {
        return new SyncDataProperties(Map.of("accounts", new SourceProperties("act_id")), mapping);
    }

    private PayloadProcessor processor(final String mapping) {
        return new PayloadProcessor(payloadService(mapping), repository, payloadRepository,
            TECHNICAL_ACCOUNT_ID, "sync-api-1");
    }

    private static PayloadServiceImpl payloadService(final String mapping) {
        return new PayloadServiceImpl(properties(mapping), new JinjaServiceImpl(), CHECKSUM_SERVICE,
            JsonMapper.builder().build());
    }

    /**
     * Gives the rows of the {@code accounts} source for a page of accounts.
     */
    @SafeVarargs
    private void rows(final List<AccountSummary> page, final Map<String, Object>... rows) {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id",
            page.stream().map(AccountSummary::actId).toList())).thenReturn(List.of(rows));
    }

    private static Map<String, Object> row(final AccountSummary account, final String email) {
        return Map.of("act_id", account.actId(), "email", email);
    }

    private AccountPayload newRow(final AccountSummary account, final String email) {
        var row = new AccountPayload();
        row.setActId(account.actId());
        row.setPayload(Map.of("email", email));
        row.setChecksum(CHECKSUM_SERVICE.compute("{\"email\":\"" + email + "\"}"));
        row.setSyncInstanceId("sync-api-1");
        row.setSyncJobExecutionId(stepExecution.getJobExecutionId());
        row.setCreatedBy(TECHNICAL_ACCOUNT_ID);
        row.setUpdatedBy(TECHNICAL_ACCOUNT_ID);
        return row;
    }
}
