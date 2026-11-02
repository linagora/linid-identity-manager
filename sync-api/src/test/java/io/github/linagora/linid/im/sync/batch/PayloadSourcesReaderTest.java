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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
import io.github.linagora.linid.im.sync.persistence.model.AccountChecksum;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.AccountLockService;
import io.github.linagora.linid.im.sync.service.AlertService;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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

    // Keys of the restart state of the reader, saved by its previous versions
    private static final String LAST_ACCOUNT_KEY = "payloadSourcesReader.last.account";

    private static final String READ_COUNT_KEY = "payloadSourcesReader.read.count";

    private static final AccountIdentity JDOE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe");

    private static final AccountIdentity ASMITH =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith");

    private static final AccountIdentity BLEE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000003"), "blee");

    private static final String JDOE_CHECKSUM = "checksum-1";

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final AccountPayloadRepository payloadRepository = mock(AccountPayloadRepository.class);

    private final SourceRepository sourceRepository = mock(SourceRepository.class);

    private final AccountLockService accountLockService = mock(AccountLockService.class);

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
        + "page, with the checksum of their payload, null without payload, and their rows in each source, read once "
        + "per page")
    void testRead() throws Exception {
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("account_payload_view", new SourceProperties("owner_id"));
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        page(ASMITH.accountId(), BLEE);
        var jdoeRow = Map.<String, Object>of("act_id", JDOE.accountId(), "email", "jdoe@example.com");
        var asmithRow = Map.<String, Object>of("act_id", ASMITH.accountId(), "email", "asmith@example.com");
        var bleeRow = Map.<String, Object>of("act_id", BLEE.accountId(), "email", "blee@example.com");
        var devRow = Map.<String, Object>of("owner_id", JDOE.accountId(), "groups", List.of("DEV"));
        var rndRow = Map.<String, Object>of("owner_id", JDOE.accountId(), "groups", List.of("RND"));
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", List.of(JDOE.accountId(), ASMITH.accountId())))
            .thenReturn(List.of(jdoeRow, asmithRow));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "owner_id",
            List.of(JDOE.accountId(), ASMITH.accountId()))).thenReturn(List.of(devRow, rndRow));
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", List.of(BLEE.accountId())))
            .thenReturn(List.of(bleeRow));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "owner_id", List.of(BLEE.accountId())))
            .thenReturn(List.of());
        var reader = reader(2, new ExecutionContext());

        assertEquals(new AccountSnapshot(JDOE, JDOE_CHECKSUM,
            Map.of("accounts", List.of(jdoeRow), "account_payload_view", List.of(devRow, rndRow))), reader.read());
        assertEquals(new AccountSnapshot(ASMITH, null,
            Map.of("accounts", List.of(asmithRow), "account_payload_view", List.of())), reader.read());
        assertEquals(new AccountSnapshot(BLEE, null,
            Map.of("accounts", List.of(bleeRow), "account_payload_view", List.of())), reader.read());
        assertNull(reader.read());
        verify(viewRepository, times(2))
            .findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(any(), any(), any());
        verify(sourceRepository, times(4)).findAllByAccountIds(any(), any(), anyCollection());
    }

    @Test
    @DisplayName("read should lock the accounts of a page before reading the checksums of their payloads and their "
        + "sources")
    void testRead_locksFirst() throws Exception {
        sources.put("accounts", new SourceProperties("act_id"));
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var accountIds = List.of(JDOE.accountId(), ASMITH.accountId());

        reader(2, new ExecutionContext()).read();

        var order = inOrder(viewRepository, accountLockService, payloadRepository, sourceRepository);
        order.verify(viewRepository).findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, LOWEST_ACCOUNT_ID, Limit.of(2));
        order.verify(accountLockService).lockAccounts(accountIds);
        order.verify(payloadRepository).findByAccountIdIn(accountIds);
        order.verify(sourceRepository).findAllByAccountIds("accounts", "act_id", accountIds);
    }

    @Test
    @DisplayName("read should give every account of a page, with a null checksum for an account without checksum "
        + "under the locks, so that the page keeps its size and the reading goes on")
    void testRead_accountWithoutChecksum() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        page(ASMITH.accountId(), BLEE);
        // jdoe deleted since its page was read: its payload was deleted with it
        when(payloadRepository.findByAccountIdIn(List.of(JDOE.accountId(), ASMITH.accountId())))
            .thenReturn(List.of());
        var reader = reader(2, new ExecutionContext());

        assertEquals(new AccountSnapshot(JDOE, null, Map.of()), reader.read());
        assertEquals(new AccountSnapshot(ASMITH, null, Map.of()), reader.read());
        assertEquals(BLEE, reader.read().account());
        assertNull(reader.read());
    }

    @Test
    @DisplayName("read should give null after a full last page, once the next page is empty")
    void testRead_fullLastPage() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var reader = reader(2, new ExecutionContext());

        assertEquals(JDOE, reader.read().account());
        assertEquals(ASMITH, reader.read().account());
        assertNull(reader.read());
        verify(viewRepository).findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, ASMITH.accountId(), Limit.of(2));
    }

    @Test
    @DisplayName("read should give the accounts without rows when there is no source")
    void testRead_noSource() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE);
        var reader = reader(2, new ExecutionContext());

        assertEquals(new AccountSnapshot(JDOE, JDOE_CHECKSUM, Map.of()), reader.read());
        assertNull(reader.read());
        verifyNoInteractions(sourceRepository);
    }

    @Test
    @DisplayName("read should give null without reading any source when there is no account")
    void testRead_noAccount() throws Exception {
        sources.put("accounts", new SourceProperties("act_id"));

        assertNull(reader(2, new ExecutionContext()).read());
        verifyNoInteractions(sourceRepository, accountLockService);
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
    @DisplayName("open and update should neither restore nor save any state for a restart, the reading starting from "
        + "the first account")
    void testNoRestartState() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var context = new ExecutionContext();
        context.putString(LAST_ACCOUNT_KEY, JDOE.accountId().toString());
        context.putInt(READ_COUNT_KEY, 1);
        var reader = reader(2, context);

        assertEquals(JDOE, reader.read().account());
        assertEquals(ASMITH, reader.read().account());
        reader.update(context);

        assertEquals(JDOE.accountId().toString(), context.getString(LAST_ACCOUNT_KEY));
        assertEquals(1, context.getInt(READ_COUNT_KEY));
        assertEquals(2, context.size());
    }

    /**
     * Gives the accounts of the page following the given account, by pages of 2 accounts, and the checksums of their
     * payloads, read under the locks: only jdoe has a payload.
     */
    private void page(final UUID after, final AccountIdentity... accounts) {
        when(viewRepository.findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, after, Limit.of(2))).thenReturn(List.of(accounts));
        when(payloadRepository.findByAccountIdIn(Stream.of(accounts).map(AccountIdentity::accountId).toList()))
            .thenReturn(Stream.of(accounts).filter(JDOE::equals)
                .map(account -> new AccountChecksum(account.accountId(), JDOE_CHECKSUM)).toList());
    }

    private PayloadSourcesReader reader(final int pageSize, final ExecutionContext context) {
        var reader = new PayloadSourcesReader(viewRepository, payloadRepository, TECHNICAL_ACCOUNT_ID, pageSize,
            new SyncDataProperties(sources, "{}"), sourceRepository, accountLockService);
        reader.open(context);
        return reader;
    }
}
