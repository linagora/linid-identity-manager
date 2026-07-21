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

package io.github.linagora.linid.im.api.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.linagora.linid.im.api.persistence.model.Account;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountMapper")
class AccountMapperTest {

    private final AccountMapper mapper = new AccountMapperImpl();

    @Test
    @DisplayName("applyUpdate should write only the editable fields and the updater identifier")
    void testApplyUpdate_shouldWriteEditableFieldsOnly() {
        UUID accountId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        Account account = new Account();
        account.setId(accountId);
        account.setExternalId("ext-001");
        account.setLastname("Doe");
        account.setFirstname("John");
        account.setEmail("john@example.com");
        account.setCreatedBy(creatorId);
        account.setUpdatedBy(creatorId);
        var record = new AccountUpdateRecord("ext-002", "Smith", "Jane", "jane@example.com", null);

        mapper.applyUpdate(account, record, updaterId);

        assertEquals("ext-002", account.getExternalId());
        assertEquals("Smith", account.getLastname());
        assertEquals("Jane", account.getFirstname());
        assertEquals("jane@example.com", account.getEmail());
        assertEquals(updaterId, account.getUpdatedBy());
        assertEquals(accountId, account.getId());
        assertEquals(creatorId, account.getCreatedBy());
    }

    @Test
    @DisplayName("applyUpdate should write extraParameters when present and preserve them when omitted")
    void testApplyUpdate_shouldWriteExtraParametersOnlyWhenPresent() {
        UUID updaterId = UUID.randomUUID();
        Account account = new Account();
        account.setExtraParameters(Map.of("kept", "value"));

        mapper.applyUpdate(account,
            new AccountUpdateRecord("ext-002", "Smith", "Jane", "jane@example.com", null),
            updaterId);
        assertEquals(Map.of("kept", "value"), account.getExtraParameters());

        mapper.applyUpdate(account,
            new AccountUpdateRecord("ext-002", "Smith", "Jane", "jane@example.com",
                Map.of("updated", true)),
            updaterId);
        assertEquals(Map.of("updated", true), account.getExtraParameters());
    }
}
