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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: PayloadStepConfig")
class PayloadStepConfigTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    @Test
    @DisplayName("payloadAccountsReader should read every account once, except the technical account, in order")
    void testAccountIdentityReader() {
        var repository = mock(AccountComputedRightViewRepository.class);
        var jdoe = new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe");
        var asmith = new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith");
        when(repository.findDistinctByActIdNotOrderByActIdAsc(TECHNICAL_ACCOUNT_ID)).thenReturn(List.of(jdoe, asmith));

        var reader = new PayloadStepConfig(repository, TECHNICAL_ACCOUNT_ID).payloadAccountsReader();

        assertEquals(jdoe, reader.read());
        assertEquals(asmith, reader.read());
        assertNull(reader.read());
    }

    @Test
    @DisplayName("constructor should reject a technical account that is not set, before any read")
    void testConstructor_technicalAccountNotSet() {
        var repository = mock(AccountComputedRightViewRepository.class);

        var exception = assertThrows(NullPointerException.class, () -> new PayloadStepConfig(repository, null));

        assertEquals("sync.technical-account-id must be set", exception.getMessage());
        verifyNoInteractions(repository);
    }
}
