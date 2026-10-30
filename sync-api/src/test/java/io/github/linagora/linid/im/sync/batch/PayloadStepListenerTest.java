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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.model.payload.PayloadOutcome;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import io.github.linagora.linid.im.sync.service.AlertService;
import io.github.linagora.linid.im.sync.service.ChecksumServiceImpl;
import io.github.linagora.linid.im.sync.service.JinjaServiceImpl;
import io.github.linagora.linid.im.sync.service.PayloadServiceImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.test.MetaDataInstanceFactory;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: PayloadStepListener")
class PayloadStepListenerTest {

    private static final String EMAIL_MAPPING = "{\"email\": {{ accounts.email | tojson }}}";

    private static final AccountSnapshot JDOE = new AccountSnapshot(
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe", null), Map.of());

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private final Logger listenerLogger = (Logger) LoggerFactory.getLogger(PayloadStepListener.class);

    private ListAppender<ILoggingEvent> alertAppender;

    private ListAppender<ILoggingEvent> listenerAppender;

    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        stepExecution = MetaDataInstanceFactory.createStepExecution(
            MetaDataInstanceFactory.createJobExecution(42L), "buildPayloadsStep", 7L);
        StepSynchronizationManager.register(stepExecution);
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
        listenerAppender = new ListAppender<>();
        listenerAppender.start();
        listenerLogger.addAppender(listenerAppender);
    }

    @AfterEach
    void tearDown() {
        StepSynchronizationManager.close();
        alertLogger.detachAppender(alertAppender);
        listenerLogger.detachAppender(listenerAppender);
    }

    @Test
    @DisplayName("beforeStep should accept a valid mapping, without alert")
    void testBeforeStep() {
        assertDoesNotThrow(() -> listener(EMAIL_MAPPING).beforeStep(stepExecution));
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("beforeStep should raise an exception after a blocking alert when the mapping is invalid")
    void testBeforeStep_invalidMapping() {
        var listener = listener("{\"email\": {{ accounts.email }}}");

        var exception = assertThrows(IllegalStateException.class, () -> listener.beforeStep(stepExecution));

        assertEquals("Invalid template sync-data.job.mapping", exception.getMessage());
        assertEquals(1, alertAppender.list.size());
        assertEquals(Level.ERROR, alertAppender.list.getFirst().getLevel());
    }

    @Test
    @DisplayName("onSkipInProcess should log the payloadError alert of the account skipped, and count it in error")
    void testOnSkipInProcess() {
        listener(EMAIL_MAPPING).onSkipInProcess(JDOE,
            new PayloadException("The account has 2 rows in the source accounts"));

        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.WARN, alert.getLevel());
        assertEquals("Account in error during payload build, keeping previous payload — account=jdoe",
            alert.getFormattedMessage());
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.IN_ERROR.key()));
    }

    @Test
    @DisplayName("count should add one to the counter of the outcome in the execution context of the current step")
    void testCount() {
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        PayloadStepListener.count(PayloadOutcome.DELETED);

        assertEquals(2, stepExecution.getExecutionContext().getInt(PayloadOutcome.INSERTED.key()));
        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.DELETED.key()));
    }

    @Test
    @DisplayName("onChunkError should restore the counters of the outcomes at the start of the chunk")
    void testOnChunkError() {
        var listener = listener(EMAIL_MAPPING);
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        listener.beforeChunk(new Chunk<>(JDOE));
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        PayloadStepListener.count(PayloadOutcome.UNCHANGED);

        listener.onChunkError(new IllegalStateException("duplicate key"), new Chunk<>());

        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.INSERTED.key()));
        assertEquals(0, stepExecution.getExecutionContext().getInt(PayloadOutcome.UNCHANGED.key()));
    }

    @Test
    @DisplayName("onChunkError should keep the counters of the chunks written when a chunk fails while its accounts are "
        + "read")
    void testOnChunkError_readError() {
        var listener = listener(EMAIL_MAPPING);
        listener.beforeChunk(new Chunk<>(JDOE));
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        listener.afterChunk(new Chunk<>());

        listener.onChunkError(new IllegalStateException("Unable to read the source accounts"), new Chunk<>());

        assertEquals(1, stepExecution.getExecutionContext().getInt(PayloadOutcome.INSERTED.key()));
    }

    @Test
    @DisplayName("afterStep should log the number of accounts of each outcome during the step, zero when none")
    void testAfterStep() {
        PayloadStepListener.count(PayloadOutcome.INSERTED);
        PayloadStepListener.count(PayloadOutcome.UPDATED);
        PayloadStepListener.count(PayloadOutcome.UPDATED);
        PayloadStepListener.count(PayloadOutcome.IN_ERROR);

        listener(EMAIL_MAPPING).afterStep(stepExecution);

        assertEquals(1, listenerAppender.list.size());
        assertEquals("Payload build of step buildPayloadsStep: 1 inserted, 2 updated, 0 unchanged, 1 in error, "
            + "0 deleted", listenerAppender.list.getFirst().getFormattedMessage());
    }

    private static PayloadStepListener listener(final String mapping) {
        return new PayloadStepListener(new PayloadServiceImpl(new SyncDataProperties(Map.of(), mapping),
            new JinjaServiceImpl(), new ChecksumServiceImpl(), JsonMapper.builder().build()));
    }
}
