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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.config.SourceProperties;
import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.AlertService;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.BadSqlGrammarException;

@DisplayName("Test class: PayloadSourcesReader")
class PayloadSourcesReaderTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final UUID LOWEST_ACCOUNT_ID = new UUID(0L, 0L);

    private static final String LAST_ACCOUNT_KEY = "payloadSourcesReader.last.account";

    private static final String READ_COUNT_KEY = "payloadSourcesReader.read.count";

    private static final AccountSummary JDOE =
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe", "checksum-1");

    private static final AccountSummary ASMITH =
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith", null);

    private static final AccountSummary BLEE =
        new AccountSummary(UUID.fromString("00000000-0000-4000-8000-000000000003"), "blee", null);

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final SourceRepository sourceRepository = mock(SourceRepository.class);

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private final Map<String, SourceProperties> sources = new LinkedHashMap<>();

    private ListAppender<ILoggingEvent> alertAppender;

    @BeforeEach
    void setUp() {
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("read should give the accounts of the view by pages starting after the last account of the previous "
        + "page, with the checksum of their payload, and their rows in each source, read once per page")
    void testRead() throws Exception {
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("account_payload_view", new SourceProperties("owner_id"));
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        page(ASMITH.actId(), BLEE);
        var jdoeRow = Map.<String, Object>of("act_id", JDOE.actId(), "email", "jdoe@example.com");
        var asmithRow = Map.<String, Object>of("act_id", ASMITH.actId(), "email", "asmith@example.com");
        var bleeRow = Map.<String, Object>of("act_id", BLEE.actId(), "email", "blee@example.com");
        var devRow = Map.<String, Object>of("owner_id", JDOE.actId(), "groups", List.of("DEV"));
        var rndRow = Map.<String, Object>of("owner_id", JDOE.actId(), "groups", List.of("RND"));
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", List.of(JDOE.actId(), ASMITH.actId())))
            .thenReturn(List.of(jdoeRow, asmithRow));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "owner_id",
            List.of(JDOE.actId(), ASMITH.actId()))).thenReturn(List.of(devRow, rndRow));
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", List.of(BLEE.actId())))
            .thenReturn(List.of(bleeRow));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "owner_id", List.of(BLEE.actId())))
            .thenReturn(List.of());
        var reader = reader(2, new ExecutionContext());

        assertEquals(new AccountSnapshot(JDOE,
            Map.of("accounts", List.of(jdoeRow), "account_payload_view", List.of(devRow, rndRow))), reader.read());
        assertEquals(new AccountSnapshot(ASMITH,
            Map.of("accounts", List.of(asmithRow), "account_payload_view", List.of())), reader.read());
        assertEquals(new AccountSnapshot(BLEE,
            Map.of("accounts", List.of(bleeRow), "account_payload_view", List.of())), reader.read());
        assertNull(reader.read());
        verify(viewRepository, times(2)).findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(any(), any(), any());
        verify(sourceRepository, times(4)).findAllByAccountIds(any(), any(), anyCollection());
    }

    @Test
    @DisplayName("read should give null after a full last page, once the next page is empty")
    void testRead_fullLastPage() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var reader = reader(2, new ExecutionContext());

        assertEquals(JDOE, reader.read().account());
        assertEquals(ASMITH, reader.read().account());
        assertNull(reader.read());
        verify(viewRepository).findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(
            TECHNICAL_ACCOUNT_ID, ASMITH.actId(), Limit.of(2));
    }

    @Test
    @DisplayName("read should give the accounts without rows when there is no source")
    void testRead_noSource() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE);
        var reader = reader(2, new ExecutionContext());

        assertEquals(new AccountSnapshot(JDOE, Map.of()), reader.read());
        assertNull(reader.read());
        verifyNoInteractions(sourceRepository);
    }

    @Test
    @DisplayName("read should give null without reading any source when there is no account")
    void testRead_noAccount() throws Exception {
        sources.put("accounts", new SourceProperties("act_id"));

        assertNull(reader(2, new ExecutionContext()).read());
        verifyNoInteractions(sourceRepository);
    }

    @Test
    @DisplayName("read should raise an exception naming the source that cannot be read, after a blocking alert")
    void testRead_unreadableSource() {
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("unknown_view", new SourceProperties("act_id"));
        page(LOWEST_ACCOUNT_ID, JDOE);
        var cause = new BadSqlGrammarException("read", "SELECT", new SQLException("relation does not exist"));
        when(sourceRepository.findAllByAccountIds(eq("unknown_view"), any(), anyCollection())).thenThrow(cause);
        var reader = reader(2, new ExecutionContext());

        var exception = assertThrows(IllegalStateException.class, reader::read);

        assertEquals("Unable to read the source unknown_view", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.ERROR, alert.getLevel());
        assertEquals("Source unreadable, payload build aborted — source=unknown_view", alert.getFormattedMessage());
    }

    @Test
    @DisplayName("update should save the last account read, not the last account of the page, and the number of "
        + "accounts read")
    void testUpdate() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var context = new ExecutionContext();
        var reader = reader(2, context);

        reader.update(context);
        assertFalse(context.containsKey(LAST_ACCOUNT_KEY));
        reader.read();
        reader.update(context);

        assertEquals(JDOE.actId().toString(), context.getString(LAST_ACCOUNT_KEY));
        assertEquals(1, context.getInt(READ_COUNT_KEY));
    }

    @Test
    @DisplayName("open should resume the reading after the last account read by the previous execution, and its "
        + "number of accounts read")
    void testOpen_restart() throws Exception {
        page(JDOE.actId(), ASMITH);
        var context = new ExecutionContext();
        context.putString(LAST_ACCOUNT_KEY, JDOE.actId().toString());
        context.putInt(READ_COUNT_KEY, 1);
        var reader = reader(2, context);

        assertEquals(ASMITH, reader.read().account());
        reader.update(context);
        assertNull(reader.read());
        verify(viewRepository).findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(
            TECHNICAL_ACCOUNT_ID, JDOE.actId(), Limit.of(2));
        assertEquals(2, context.getInt(READ_COUNT_KEY));
    }

    @Test
    @DisplayName("open and update should neither restore nor save the last account read when the state is not saved")
    void testSaveStateDisabled() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE);
        var context = new ExecutionContext();
        context.putString(LAST_ACCOUNT_KEY, ASMITH.actId().toString());
        var reader = reader(2, context, false);

        assertEquals(JDOE, reader.read().account());
        reader.update(context);

        assertEquals(ASMITH.actId().toString(), context.getString(LAST_ACCOUNT_KEY));
    }

    private void page(final UUID after, final AccountSummary... accounts) {
        when(viewRepository.findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(
            TECHNICAL_ACCOUNT_ID, after, Limit.of(2))).thenReturn(List.of(accounts));
    }

    private PayloadSourcesReader reader(final int pageSize, final ExecutionContext context) {
        return reader(pageSize, context, true);
    }

    private PayloadSourcesReader reader(final int pageSize, final ExecutionContext context, final boolean saveState) {
        var reader = new PayloadSourcesReader(viewRepository, TECHNICAL_ACCOUNT_ID, pageSize,
            new SyncDataProperties(sources, "{}"), sourceRepository);
        reader.setSaveState(saveState);
        reader.open(context);
        return reader;
    }
}
