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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.model.payload.PayloadOutcome;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.service.AlertService;
import io.github.linagora.linid.im.sync.service.ChecksumService;
import io.github.linagora.linid.im.sync.service.ChecksumServiceImpl;
import io.github.linagora.linid.im.sync.service.JinjaServiceImpl;
import io.github.linagora.linid.im.sync.service.PayloadService;
import io.github.linagora.linid.im.sync.service.PayloadServiceImpl;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: PayloadProcessor")
class PayloadProcessorTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-0000000000ff");

    private static final UUID JDOE = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final String INSTANCE_ID = "sync-api-1";

    private static final long JOB_EXECUTION_ID = 42L;

    private static final String EMAIL_MAPPING = "{\"email\": {{ accounts.email | tojson }}}";

    private static final Map<String, List<Map<String, Object>>> ROWS_BY_SOURCE =
        Map.of("accounts", List.of(Map.of("email", "jdoe@example.com")));

    private static final ChecksumService CHECKSUM_SERVICE = new ChecksumServiceImpl();

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final AccountPayloadRepository payloadRepository = mock(AccountPayloadRepository.class);

    private ListAppender<ILoggingEvent> alertAppender;

    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        stepExecution = MetaDataInstanceFactory.createStepExecution(
            MetaDataInstanceFactory.createJobExecution(JOB_EXECUTION_ID), "buildPayloadsStep", 7L);
        StepSynchronizationManager.register(stepExecution);
        when(viewRepository.existsByActId(JDOE)).thenReturn(true);
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        StepSynchronizationManager.close();
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("process should give a new row for an account without payload, with the technical account as author, "
        + "the instance and the job execution")
    void testProcess_newPayload() {
        var row = processor(EMAIL_MAPPING).process(item(ROWS_BY_SOURCE, null));

        assertEquals(row(Map.of("email", "jdoe@example.com"),
            CHECKSUM_SERVICE.compute("{\"email\":\"jdoe@example.com\"}")), row);
        verifyNoInteractions(payloadRepository);
    }

    @Test
    @DisplayName("process should update the current row of a changed payload, with the technical account as updater, "
        + "the instance and the job execution, keeping its author, without checking that the account still exists")
    void testProcess_changedPayload() {
        var current = new AccountPayload();
        current.setAcpId(UUID.randomUUID());
        current.setActId(JDOE);
        current.setChecksum("previous");
        current.setCreatedBy(OTHER_ACCOUNT_ID);
        current.setUpdatedBy(OTHER_ACCOUNT_ID);
        when(payloadRepository.findByActId(JDOE)).thenReturn(Optional.of(current));

        var row = processor(EMAIL_MAPPING).process(item(ROWS_BY_SOURCE, "previous"));

        assertSame(current, row);
        var expected = row(Map.of("email", "jdoe@example.com"),
            CHECKSUM_SERVICE.compute("{\"email\":\"jdoe@example.com\"}"));
        expected.setAcpId(current.getAcpId());
        expected.setCreatedBy(OTHER_ACCOUNT_ID);
        assertEquals(expected, row);
        verify(viewRepository, never()).existsByActId(any());
    }

    @Test
    @DisplayName("process should filter out an unchanged payload, without reading its row")
    void testProcess_unchangedPayload() {
        assertNull(processor(EMAIL_MAPPING).process(
            item(ROWS_BY_SOURCE, CHECKSUM_SERVICE.compute("{\"email\":\"jdoe@example.com\"}"))));
        verifyNoInteractions(payloadRepository);
    }

    @Test
    @DisplayName("process should filter out an account without payload deleted since the accounts were read")
    void testProcess_deletedAccount() {
        when(viewRepository.existsByActId(JDOE)).thenReturn(false);

        assertNull(processor(EMAIL_MAPPING).process(item(ROWS_BY_SOURCE, null)));
        verifyNoInteractions(payloadRepository);
    }

    @Test
    @DisplayName("process should filter out an account with a payload deleted since the accounts were read, its row "
        + "being deleted with it, without checking that the account still exists")
    void testProcess_deletedAccountWithPayload() {
        when(payloadRepository.findByActId(JDOE)).thenReturn(Optional.empty());

        assertNull(processor(EMAIL_MAPPING).process(item(ROWS_BY_SOURCE, "previous")));
        verify(viewRepository, never()).existsByActId(any());
    }

    @Test
    @DisplayName("process should let the PayloadException of an account in error through, without reading its row, "
        + "without alert")
    void testProcess_accountInError() {
        var processor = processor(EMAIL_MAPPING);
        var item = item(Map.of("accounts", List.of(
            Map.of("email", "jdoe@example.com"), Map.of("email", "john@example.com"))), "previous");

        var exception = assertThrows(PayloadException.class, () -> processor.process(item));

        assertEquals("The account has 2 rows in the source accounts", exception.getMessage());
        verifyNoInteractions(viewRepository, payloadRepository);
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("process should write the host name as instance when SYNC_INSTANCE_ID is not set, the default of "
        + "sync.instance-id in application.yaml")
    void testProcess_defaultInstance() throws Exception {
        var environment = new StandardEnvironment();
        var sources = environment.getPropertySources();
        // Without the environment of the machine running the tests, which may set SYNC_INSTANCE_ID
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addFirst(new MapPropertySource("environment",
            Map.of("SYNC_TECHNICAL_ACCOUNT_ID", TECHNICAL_ACCOUNT_ID.toString())));
        new YamlPropertySourceLoader().load("application.yaml", new ClassPathResource("application.yaml"))
            .forEach(sources::addLast);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.registerBean(PayloadService.class, () -> payloadService(EMAIL_MAPPING));
            context.registerBean(AccountComputedRightViewRepository.class, () -> viewRepository);
            context.registerBean(AccountPayloadRepository.class, () -> payloadRepository);
            context.register(PayloadProcessor.class);
            context.refresh();

            var row = context.getBean(PayloadProcessor.class).process(item(ROWS_BY_SOURCE, null));

            assertEquals(InetAddress.getLocalHost().getHostName(), row.getSyncInstanceId());
        }
    }

    @Test
    @DisplayName("process should count the outcome of each account in the execution context of the step")
    void testProcess_outcomes() {
        var processor = processor(EMAIL_MAPPING);
        var checksum = CHECKSUM_SERVICE.compute("{\"email\":\"jdoe@example.com\"}");
        processor.process(item(ROWS_BY_SOURCE, null));
        processor.process(item(ROWS_BY_SOURCE, checksum));
        when(payloadRepository.findByActId(JDOE)).thenReturn(Optional.of(new AccountPayload()));
        processor.process(item(ROWS_BY_SOURCE, "previous"));
        when(payloadRepository.findByActId(JDOE)).thenReturn(Optional.empty());
        processor.process(item(ROWS_BY_SOURCE, "previous"));

        var context = stepExecution.getExecutionContext();
        assertEquals(1, context.getInt(PayloadOutcome.INSERTED.key()));
        assertEquals(1, context.getInt(PayloadOutcome.UNCHANGED.key()));
        assertEquals(1, context.getInt(PayloadOutcome.UPDATED.key()));
        assertEquals(1, context.getInt(PayloadOutcome.DELETED.key()));
    }

    private PayloadProcessor processor(final String mapping) {
        return new PayloadProcessor(payloadService(mapping), viewRepository, payloadRepository, TECHNICAL_ACCOUNT_ID,
            INSTANCE_ID);
    }

    private static PayloadServiceImpl payloadService(final String mapping) {
        return new PayloadServiceImpl(new SyncDataProperties(Map.of(), mapping), new JinjaServiceImpl(),
            CHECKSUM_SERVICE, JsonMapper.builder().build());
    }

    private static AccountSnapshot item(final Map<String, List<Map<String, Object>>> rowsBySource,
                                       final String payloadChecksum) {
        return new AccountSnapshot(new AccountSummary(JDOE, "jdoe", payloadChecksum), rowsBySource);
    }

    private static AccountPayload row(final Map<String, Object> payload, final String checksum) {
        var row = new AccountPayload();
        row.setActId(JDOE);
        row.setPayload(payload);
        row.setChecksum(checksum);
        row.setSyncInstanceId(INSTANCE_ID);
        row.setSyncJobExecutionId(JOB_EXECUTION_ID);
        row.setCreatedBy(TECHNICAL_ACCOUNT_ID);
        row.setUpdatedBy(TECHNICAL_ACCOUNT_ID);
        return row;
    }
}
