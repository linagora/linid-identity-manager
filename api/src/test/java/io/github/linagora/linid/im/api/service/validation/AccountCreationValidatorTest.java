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
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AccountCreationValidator")
class AccountCreationValidatorTest {

    private static final String EMAIL = "john@example.com";

    @Mock
    private CommonMapper commonMapper;

    @InjectMocks
    private AccountCreationValidator validator;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        this.now = OffsetDateTime.now();
    }

    private AccountRecord recordWithStart(final OffsetDateTime start) {
        return new AccountRecord("ext-001", "Doe", "John", EMAIL,
            new PeriodRecord(start, null), UUID.randomUUID());
    }

    // -------------------------------------------------------------------------
    // validate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validate should accept a valid creation request")
    void testValidate_shouldAcceptValidRequest() {
        AccountRecord account = recordWithStart(now.plusDays(1));
        when(commonMapper.startOf(account.validityPeriod())).thenReturn(now.plusDays(1));

        assertDoesNotThrow(() -> validator.validate(account));
    }

    @Test
    @DisplayName("validate should throw 400 when validity period start is null")
    void testValidate_shouldThrowWhenStartNull() {
        AccountRecord account = recordWithStart(null);
        when(commonMapper.startOf(account.validityPeriod())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(account));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.creation.validity_period_start_required", ex.getError().key());
    }

    @Test
    @DisplayName("validate should throw 400 when validity period start is in the past")
    void testValidate_shouldThrowWhenStartInPast() {
        AccountRecord account = recordWithStart(now.minusDays(1));
        when(commonMapper.startOf(account.validityPeriod())).thenReturn(now.minusDays(1));

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(account));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.creation.validity_period_start_in_past", ex.getError().key());
    }

    // -------------------------------------------------------------------------
    // ensureValidityPeriodStartNotNull
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ensureValidityPeriodStartNotNull should throw 400 when start is null")
    void testEnsureValidityPeriodStartNotNull_shouldThrowWhenNull() {
        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityPeriodStartNotNull(null, EMAIL));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.creation.validity_period_start_required", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityPeriodStartNotNull should not throw when start is set")
    void testEnsureValidityPeriodStartNotNull_shouldNotThrowWhenSet() {
        assertDoesNotThrow(() -> validator.ensureValidityPeriodStartNotNull(now.plusDays(1), EMAIL));
    }

    // -------------------------------------------------------------------------
    // ensureValidityPeriodStartNotInPast
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ensureValidityPeriodStartNotInPast should throw 400 when start is in the past")
    void testEnsureValidityPeriodStartNotInPast_shouldThrowWhenInPast() {
        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityPeriodStartNotInPast(now.minusDays(1), EMAIL));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.creation.validity_period_start_in_past", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityPeriodStartNotInPast should accept a future start")
    void testEnsureValidityPeriodStartNotInPast_shouldAcceptFutureStart() {
        assertDoesNotThrow(() -> validator.ensureValidityPeriodStartNotInPast(now.plusDays(1), EMAIL));
    }

    @Test
    @DisplayName("ensureValidityPeriodStartNotInPast should accept a start strictly in the future (now + 1 min)")
    void testEnsureValidityPeriodStartNotInPast_shouldAcceptBoundary() {
        assertDoesNotThrow(() -> validator.ensureValidityPeriodStartNotInPast(now.plusMinutes(1), EMAIL));
    }

    @Test
    @DisplayName("ensureValidityPeriodStartNotInPast should not throw when start is null")
    void testEnsureValidityPeriodStartNotInPast_shouldNotThrowWhenNull() {
        assertDoesNotThrow(() -> validator.ensureValidityPeriodStartNotInPast(null, EMAIL));
    }
}
