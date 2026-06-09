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

package io.github.linagora.linid.im.api.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.api.model.account.AccountDeactivationRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountDeactivationValidator")
class AccountDeactivationValidatorTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private AccountDeactivationValidator validator;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        this.validator = new AccountDeactivationValidator(new CommonMapper(), new PeriodValidator());
        this.now = OffsetDateTime.now();
    }

    private static Range<ZonedDateTime> closedOpen(final OffsetDateTime start, final OffsetDateTime end) {
        return Range.closedOpen(start.toZonedDateTime(), end.toZonedDateTime());
    }

    private static AccountDeactivationRecord record(final OffsetDateTime deactivationAt) {
        return new AccountDeactivationRecord(deactivationAt, "REASON", "SUBREASON", "comment");
    }

    @Test
    @DisplayName("validate should accept a future deactivation timestamp")
    void testValidate_shouldAcceptValidRequest() {
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(closedOpen(now.minusDays(5), now.plusDays(30)));
        AccountDeactivationRecord record = record(now.plusDays(10));

        assertDoesNotThrow(() -> validator.validate(status, record, ACCOUNT_ID));
    }

    @Test
    @DisplayName("validate should throw 400 when deactivation timestamp is in the past")
    void testValidate_shouldThrowWhenDeactivationInPast() {
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(closedOpen(now.minusDays(30), now.plusDays(30)));
        AccountDeactivationRecord record = record(now.minusDays(1));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.validate(status, record, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.deactivation_in_past", ex.getError().key());
    }

    @Test
    @DisplayName("validate should throw 400 when deactivation is before the persisted validity start")
    void testValidate_shouldThrowWhenBeforeValidityStart() {
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(closedOpen(now.plusDays(10), now.plusDays(30)));
        AccountDeactivationRecord record = record(now.plusDays(5));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.validate(status, record, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.deactivation_before_validity_start", ex.getError().key());
    }
}
