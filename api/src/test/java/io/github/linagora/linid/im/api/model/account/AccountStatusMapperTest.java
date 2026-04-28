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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountStatusMapper")
class AccountStatusMapperTest {

    private AccountStatusMapper mapper;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        AccountStatusMapperImpl impl = new AccountStatusMapperImpl();
        Field commonMapperField = AccountStatusMapperImpl.class.getDeclaredField("commonMapper");
        commonMapperField.setAccessible(true);
        commonMapperField.set(impl, new CommonMapper());
        this.mapper = impl;
    }

    @Test
    @DisplayName("update should overwrite every field, including nulls")
    void testUpdate_shouldOverwriteAllFieldsIncludingNulls() {
        AccountStatus target = new AccountStatus();
        target.setStatusReason("PREVIOUS");
        target.setStatusComment("keep me");
        target.setActivationAt(OffsetDateTime.parse("2025-01-01T00:00:00Z"));

        AccountStatusRecord record = new AccountStatusRecord(
            new PeriodRecord(OffsetDateTime.parse("2025-02-01T00:00:00Z"), null),
            null,
            null,
            "NEW_REASON",
            null,
            null);

        mapper.update(target, record);

        assertNotNull(target.getValidityPeriod());
        assertTrue(target.getValidityPeriod().hasLowerBound());
        assertEquals(OffsetDateTime.parse("2025-02-01T00:00:00Z").toZonedDateTime(),
            target.getValidityPeriod().lower());
        assertFalse(target.getValidityPeriod().hasUpperBound());
        assertNull(target.getSuspensionPeriod());
        assertNull(target.getActivationAt());
        assertEquals("NEW_REASON", target.getStatusReason());
        assertNull(target.getStatusSubreason());
        assertNull(target.getStatusComment());
    }

    @Test
    @DisplayName("update should be a no-op when the record is null")
    void testUpdate_shouldBeNoOpOnNullRecord() {
        AccountStatus target = new AccountStatus();
        target.setStatusReason("KEEP");

        mapper.update(target, null);

        assertEquals("KEEP", target.getStatusReason());
    }
}
