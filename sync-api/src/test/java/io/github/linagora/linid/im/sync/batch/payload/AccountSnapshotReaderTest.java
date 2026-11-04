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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountSnapshotReader")
class AccountSnapshotReaderTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final AccountIdentity JDOE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe");

    private static final AccountSnapshot JDOE_SNAPSHOT = new AccountSnapshot(JDOE, "checksum-1", Map.of());

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final AccountSnapshotLoader loader = mock(AccountSnapshotLoader.class);

    @Test
    @DisplayName("read should give the snapshot of the account at the first read, then null, reading and loading the "
        + "account once")
    void testRead() {
        when(viewRepository.findDistinctByAccountId(JDOE.accountId())).thenReturn(Optional.of(JDOE));
        when(loader.load(JDOE)).thenReturn(JDOE_SNAPSHOT);
        var reader = reader(JDOE.accountId());

        assertEquals(JDOE_SNAPSHOT, reader.read());
        assertNull(reader.read());
        assertNull(reader.read());
        verify(viewRepository).findDistinctByAccountId(JDOE.accountId());
        verify(loader).load(JDOE);
    }

    @Test
    @DisplayName("read should read and load the account at the first read, not when the reader is created")
    void testRead_loadedAtFirstRead() {
        when(viewRepository.findDistinctByAccountId(JDOE.accountId())).thenReturn(Optional.of(JDOE));
        when(loader.load(JDOE)).thenReturn(JDOE_SNAPSHOT);

        var reader = reader(JDOE.accountId());

        verifyNoInteractions(viewRepository, loader);
        assertEquals(JDOE_SNAPSHOT, reader.read());
    }

    @Test
    @DisplayName("read should give null when the account has no row in the view, without loading it nor reading the "
        + "view again")
    void testRead_accountWithoutRow() {
        when(viewRepository.findDistinctByAccountId(JDOE.accountId())).thenReturn(Optional.empty());
        var reader = reader(JDOE.accountId());

        assertNull(reader.read());
        assertNull(reader.read());
        verify(viewRepository).findDistinctByAccountId(JDOE.accountId());
        verifyNoInteractions(loader);
    }

    @Test
    @DisplayName("read should give null for the technical account, without any query")
    void testRead_technicalAccount() {
        var reader = reader(TECHNICAL_ACCOUNT_ID);

        assertNull(reader.read());
        assertNull(reader.read());
        verifyNoInteractions(viewRepository, loader);
    }

    private AccountSnapshotReader reader(final UUID accountId) {
        return new AccountSnapshotReader(accountId, TECHNICAL_ACCOUNT_ID, viewRepository, loader);
    }
}
