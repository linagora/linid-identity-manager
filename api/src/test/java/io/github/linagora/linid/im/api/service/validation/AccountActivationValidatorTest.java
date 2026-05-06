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

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
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

@DisplayName("Test class: AccountActivationValidator")
class AccountActivationValidatorTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private AccountActivationValidator validator;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        this.validator = new AccountActivationValidator(new CommonMapper());
        this.now = OffsetDateTime.now();
    }

    private static Range<ZonedDateTime> closedOpen(final OffsetDateTime start, final OffsetDateTime end) {
        return Range.closedOpen(start.toZonedDateTime(), end.toZonedDateTime());
    }

    @Test
    @DisplayName("validate should accept a valid activation request")
    void testValidate_shouldAcceptValidRequest() {
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(closedOpen(now.minusDays(5), now.plusDays(30)));
        AccountActivationRecord record = new AccountActivationRecord(now.minusHours(1));

        assertDoesNotThrow(() -> validator.validate(status, record, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureNotAlreadyActivated should throw 400 when activationAt is already set")
    void testEnsureNotAlreadyActivated_shouldThrowWhenAlreadyActivated() {
        AccountStatus status = new AccountStatus();
        status.setActivationAt(now.minusDays(1));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureNotAlreadyActivated(status, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.already_activated", ex.getError().key());
    }

    @Test
    @DisplayName("ensureNotAlreadyActivated should not throw when activationAt is null")
    void testEnsureNotAlreadyActivated_shouldNotThrowWhenNull() {
        assertDoesNotThrow(() -> validator.ensureNotAlreadyActivated(new AccountStatus(), ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityStartExists should throw 400 when validity period is null")
    void testEnsureValidityStartExists_shouldThrowWhenValidityNull() {
        AccountStatus status = new AccountStatus();

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityStartExists(status, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.validity_required", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityStartExists should throw 400 when validity has no lower bound")
    void testEnsureValidityStartExists_shouldThrowWhenLowerBoundMissing() {
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(Range.infiniteOpen(now.plusDays(30).toZonedDateTime()));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityStartExists(status, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.validity_required", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityStartExists should return the lower bound as OffsetDateTime")
    void testEnsureValidityStartExists_shouldReturnLowerBound() {
        OffsetDateTime start = now.minusDays(5);
        AccountStatus status = new AccountStatus();
        status.setValidityPeriod(closedOpen(start, now.plusDays(30)));

        OffsetDateTime result = validator.ensureValidityStartExists(status, ACCOUNT_ID);

        assertEquals(start.toInstant(), result.toInstant());
    }

    @Test
    @DisplayName("ensureValidityNotInFuture should throw 400 when validity start is in the future")
    void testEnsureValidityNotInFuture_shouldThrowWhenInFuture() {
        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityNotInFuture(now.plusDays(1), now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.validity_in_future", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityNotInFuture should accept boundary equality (start == now)")
    void testEnsureValidityNotInFuture_shouldAcceptBoundary() {
        assertDoesNotThrow(() -> validator.ensureValidityNotInFuture(now, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureActivationAfterStart should throw 400 when activationAt is before validity start")
    void testEnsureActivationAfterStart_shouldThrowWhenBeforeStart() {
        OffsetDateTime validityStart = now.minusDays(5);
        AccountActivationRecord record = new AccountActivationRecord(now.minusDays(10));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureActivationAfterStart(record, validityStart, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.before_validity_start", ex.getError().key());
    }

    @Test
    @DisplayName("ensureActivationAfterStart should accept boundary equality (activationAt == start)")
    void testEnsureActivationAfterStart_shouldAcceptBoundary() {
        OffsetDateTime validityStart = now.minusDays(5);
        AccountActivationRecord record = new AccountActivationRecord(validityStart);

        assertDoesNotThrow(() -> validator.ensureActivationAfterStart(record, validityStart, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureActivationNotInFuture should throw 400 when activationAt is in the future")
    void testEnsureActivationNotInFuture_shouldThrowWhenInFuture() {
        AccountActivationRecord record = new AccountActivationRecord(now.plusHours(1));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureActivationNotInFuture(record, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.in_future", ex.getError().key());
    }

    @Test
    @DisplayName("ensureActivationNotInFuture should accept boundary equality (activationAt == now)")
    void testEnsureActivationNotInFuture_shouldAcceptBoundary() {
        AccountActivationRecord record = new AccountActivationRecord(now);

        assertDoesNotThrow(() -> validator.ensureActivationNotInFuture(record, now, ACCOUNT_ID));
    }
}
