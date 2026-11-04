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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.AccountLockService;
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
import org.springframework.jdbc.BadSqlGrammarException;

@DisplayName("Test class: AccountSnapshotLoader")
class AccountSnapshotLoaderTest {

    private static final AccountIdentity JDOE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe");

    private static final AccountIdentity ASMITH =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith");

    private static final List<UUID> ACCOUNT_IDS = List.of(JDOE.accountId(), ASMITH.accountId());

    private static final String JDOE_CHECKSUM = "checksum-1";

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
        // only jdoe has a payload
        when(payloadRepository.findByAccountIdIn(ACCOUNT_IDS)).thenReturn(List.of(new AccountChecksum(JDOE.accountId(),
            JDOE_CHECKSUM)));
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("load should give the accounts in their order, with the checksum of their payload, null without "
        + "payload, and their rows in each source, each source being read once for all the accounts, and an account "
        + "with several rows in a source being given as it is")
    void testLoad() {
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("account_payload_view", new SourceProperties("owner_id"));
        var jdoeRow = Map.<String, Object>of("act_id", JDOE.accountId(), "email", "jdoe@example.com");
        var asmithRow = Map.<String, Object>of("act_id", ASMITH.accountId(), "email", "asmith@example.com");
        var devRow = Map.<String, Object>of("owner_id", JDOE.accountId(), "groups", List.of("DEV"));
        var rndRow = Map.<String, Object>of("owner_id", JDOE.accountId(), "groups", List.of("RND"));
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", ACCOUNT_IDS))
            .thenReturn(List.of(jdoeRow, asmithRow));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "owner_id", ACCOUNT_IDS))
            .thenReturn(List.of(devRow, rndRow));

        assertEquals(List.of(
            new AccountSnapshot(JDOE, JDOE_CHECKSUM,
                Map.of("accounts", List.of(jdoeRow), "account_payload_view", List.of(devRow, rndRow))),
            new AccountSnapshot(ASMITH, null,
                Map.of("accounts", List.of(asmithRow), "account_payload_view", List.of()))),
            loader().load(List.of(JDOE, ASMITH)));
        verify(sourceRepository, times(2)).findAllByAccountIds(any(), any(), anyCollection());
    }

    @Test
    @DisplayName("load should lock the accounts before reading the checksums of their payloads and their sources")
    void testLoad_locksFirst() {
        sources.put("accounts", new SourceProperties("act_id"));

        loader().load(List.of(JDOE, ASMITH));

        var order = inOrder(accountLockService, payloadRepository, sourceRepository);
        order.verify(accountLockService).lockAccounts(ACCOUNT_IDS);
        order.verify(payloadRepository).findByAccountIdIn(ACCOUNT_IDS);
        order.verify(sourceRepository).findAllByAccountIds("accounts", "act_id", ACCOUNT_IDS);
    }

    @Test
    @DisplayName("load should give every account, with a null checksum for an account without checksum under the "
        + "locks, so that a page keeps its size")
    void testLoad_accountWithoutChecksum() {
        // jdoe deleted since it was read: its payload was deleted with it
        when(payloadRepository.findByAccountIdIn(ACCOUNT_IDS)).thenReturn(List.of());

        assertEquals(List.of(new AccountSnapshot(JDOE, null, Map.of()), new AccountSnapshot(ASMITH, null, Map.of())),
            loader().load(List.of(JDOE, ASMITH)));
    }

    @Test
    @DisplayName("load should give the accounts without rows when there is no source")
    void testLoad_noSource() {
        assertEquals(List.of(new AccountSnapshot(JDOE, JDOE_CHECKSUM, Map.of()),
            new AccountSnapshot(ASMITH, null, Map.of())), loader().load(List.of(JDOE, ASMITH)));
        verifyNoInteractions(sourceRepository);
    }

    @Test
    @DisplayName("load should give no snapshot, without any lock nor query, when there is no account")
    void testLoad_noAccount() {
        sources.put("accounts", new SourceProperties("act_id"));

        assertEquals(List.of(), loader().load(List.of()));
        verifyNoInteractions(accountLockService, payloadRepository, sourceRepository);
    }

    @Test
    @DisplayName("load should raise an exception naming the source that cannot be read, after a blocking alert")
    void testLoad_unreadableSource() {
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("unknown_view", new SourceProperties("act_id"));
        var cause = new BadSqlGrammarException("read", "SELECT", new SQLException("relation does not exist"));
        when(sourceRepository.findAllByAccountIds(eq("unknown_view"), any(), anyCollection())).thenThrow(cause);
        var loader = loader();
        var accounts = List.of(JDOE, ASMITH);

        var exception = assertThrows(IllegalStateException.class, () -> loader.load(accounts));

        assertEquals("Unable to read the source unknown_view", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.ERROR, alert.getLevel());
        assertEquals("Source unreadable, payload build aborted — source=unknown_view", alert.getFormattedMessage());
    }

    private AccountSnapshotLoader loader() {
        return new AccountSnapshotLoader(payloadRepository, new SyncDataProperties(sources, "{}"), sourceRepository,
            accountLockService);
    }
}
