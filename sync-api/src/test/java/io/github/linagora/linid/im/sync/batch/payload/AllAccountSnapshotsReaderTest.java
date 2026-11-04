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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.data.domain.Limit;

@DisplayName("Test class: AllAccountSnapshotsReader")
class AllAccountSnapshotsReaderTest {

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

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final AccountSnapshotLoader loader = mock(AccountSnapshotLoader.class);

    @Test
    @DisplayName("read should give the snapshots of the accounts of the view, except the technical account, by pages "
        + "starting after the last account of the previous page, each page being loaded once")
    void testRead() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        page(ASMITH.accountId(), BLEE);
        var reader = reader(new ExecutionContext());

        assertEquals(snapshot(JDOE), reader.read());
        assertEquals(snapshot(ASMITH), reader.read());
        assertEquals(snapshot(BLEE), reader.read());
        assertNull(reader.read());
        var order = inOrder(viewRepository, loader);
        order.verify(viewRepository).findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, LOWEST_ACCOUNT_ID, Limit.of(2));
        order.verify(loader).load(List.of(JDOE, ASMITH));
        order.verify(viewRepository).findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, ASMITH.accountId(), Limit.of(2));
        order.verify(loader).load(List.of(BLEE));
    }

    @Test
    @DisplayName("read should give null after a full last page, once the next page is empty")
    void testRead_fullLastPage() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        page(ASMITH.accountId());
        var reader = reader(new ExecutionContext());

        assertEquals(snapshot(JDOE), reader.read());
        assertEquals(snapshot(ASMITH), reader.read());
        assertNull(reader.read());
        verify(viewRepository).findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, ASMITH.accountId(), Limit.of(2));
    }

    @Test
    @DisplayName("read should give null when there is no account")
    void testRead_noAccount() throws Exception {
        page(LOWEST_ACCOUNT_ID);

        assertNull(reader(new ExecutionContext()).read());
        verify(loader).load(List.of());
    }

    @Test
    @DisplayName("open and update should neither restore nor save any state for a restart, the reading starting from "
        + "the first account")
    void testNoRestartState() throws Exception {
        page(LOWEST_ACCOUNT_ID, JDOE, ASMITH);
        var context = new ExecutionContext();
        context.putString(LAST_ACCOUNT_KEY, JDOE.accountId().toString());
        context.putInt(READ_COUNT_KEY, 1);
        var reader = reader(context);

        assertEquals(snapshot(JDOE), reader.read());
        assertEquals(snapshot(ASMITH), reader.read());
        reader.update(context);

        assertEquals(JDOE.accountId().toString(), context.getString(LAST_ACCOUNT_KEY));
        assertEquals(1, context.getInt(READ_COUNT_KEY));
        assertEquals(2, context.size());
    }

    /**
     * Gives the accounts of the page following the given account, by pages of 2 accounts, and their snapshots.
     */
    private void page(final UUID after, final AccountIdentity... accounts) {
        var page = List.of(accounts);
        when(viewRepository.findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
            TECHNICAL_ACCOUNT_ID, after, Limit.of(2))).thenReturn(page);
        when(loader.load(page)).thenReturn(Stream.of(accounts).map(AllAccountSnapshotsReaderTest::snapshot).toList());
    }

    private static AccountSnapshot snapshot(final AccountIdentity account) {
        return new AccountSnapshot(account, null, Map.of());
    }

    private AllAccountSnapshotsReader reader(final ExecutionContext context) {
        var reader = new AllAccountSnapshotsReader(viewRepository, TECHNICAL_ACCOUNT_ID, 2, loader);
        reader.open(context);
        return reader;
    }
}
