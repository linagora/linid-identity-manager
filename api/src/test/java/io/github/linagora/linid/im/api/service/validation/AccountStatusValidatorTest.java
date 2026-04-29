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

import io.github.linagora.linid.im.api.model.account.AccountStatusRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountStatusValidator")
class AccountStatusValidatorTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private AccountStatusValidator validator;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        this.validator = new AccountStatusValidator(new CommonMapper(), new PeriodValidator());
        this.now = OffsetDateTime.now();
    }

    private static Range<ZonedDateTime> closedOpen(final OffsetDateTime start, final OffsetDateTime end) {
        return Range.closedOpen(start.toZonedDateTime(), end.toZonedDateTime());
    }

    private static AccountStatusRecord record(final PeriodRecord validity, final PeriodRecord suspension) {
        return new AccountStatusRecord(validity, suspension, null, null, null, null);
    }

    private static AccountStatusRecord recordWithActivationAt(final OffsetDateTime activationAt) {
        return new AccountStatusRecord(null, null, activationAt, null, null, null);
    }

    @Test
    @DisplayName("validate should accept a fully valid status update")
    void testValidate_shouldAcceptValidRequest() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(1), now.plusDays(30)),
            new PeriodRecord(now.plusDays(5), now.plusDays(10))
        );

        assertDoesNotThrow(() -> validator.validate(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("validate should accept open-ended validity (end null) without suspension")
    void testValidate_shouldAcceptOpenEndedValidity() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(1), null), null);

        assertDoesNotThrow(() -> validator.validate(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("validate should accept idempotent calls preserving a past validity start")
    void testValidate_shouldAcceptIdempotentPastStart() {
        OffsetDateTime past = now.minusDays(10);
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(past, now.plusDays(30)));
        AccountStatusRecord input = record(new PeriodRecord(past, now.plusDays(60)), null);

        assertDoesNotThrow(() -> validator.validate(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureActivationAtNotProvided should throw 400 when activationAt is set")
    void testEnsureActivationAtNotProvided_shouldThrowWhenSet() {
        AccountStatusRecord input = recordWithActivationAt(now.minusHours(1));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureActivationAtNotProvided(input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation_at_read_only", ex.getError().key());
    }

    @Test
    @DisplayName("ensureActivationAtNotProvided should accept null activationAt")
    void testEnsureActivationAtNotProvided_shouldAcceptNull() {
        AccountStatusRecord input = recordWithActivationAt(null);

        assertDoesNotThrow(() -> validator.ensureActivationAtNotProvided(input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityPeriodCoherent should throw 400 when start is after end")
    void testEnsureValidityPeriodCoherent_shouldThrowWhenStartAfterEnd() {
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(10), now.plusDays(5)), null);

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityPeriodCoherent(input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_period_invalid", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityPeriodCoherent should accept boundary equality (start == end)")
    void testEnsureValidityPeriodCoherent_shouldAcceptBoundary() {
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(5), now.plusDays(5)), null);

        assertDoesNotThrow(() -> validator.ensureValidityPeriodCoherent(input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityPeriodCoherent should skip when one bound is null")
    void testEnsureValidityPeriodCoherent_shouldSkipWhenOpenEnded() {
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(1), null), null);

        assertDoesNotThrow(() -> validator.ensureValidityPeriodCoherent(input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionPeriodCoherent should throw 400 when start is after end")
    void testEnsureSuspensionPeriodCoherent_shouldThrowWhenStartAfterEnd() {
        AccountStatusRecord input = record(null, new PeriodRecord(now.plusDays(10), now.plusDays(5)));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionPeriodCoherent(input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_period_invalid", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionPeriodCoherent should accept boundary equality (start == end)")
    void testEnsureSuspensionPeriodCoherent_shouldAcceptBoundary() {
        AccountStatusRecord input = record(null, new PeriodRecord(now.plusDays(5), now.plusDays(5)));

        assertDoesNotThrow(() -> validator.ensureSuspensionPeriodCoherent(input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityStartNotChangedWhenPast should throw 400 when changing a past start")
    void testEnsureValidityStartNotChangedWhenPast_shouldThrowWhenChanged() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.minusDays(10), now.plusDays(30)));
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(1), now.plusDays(30)), null);

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityStartNotChangedWhenPast(current, input, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_start_frozen", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityStartNotChangedWhenPast should throw 400 when clearing a past start")
    void testEnsureValidityStartNotChangedWhenPast_shouldThrowWhenCleared() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.minusDays(10), now.plusDays(30)));
        AccountStatusRecord input = record(null, null);

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityStartNotChangedWhenPast(current, input, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_start_frozen", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityStartNotChangedWhenPast should accept idempotent same-value call")
    void testEnsureValidityStartNotChangedWhenPast_shouldAcceptIdempotent() {
        OffsetDateTime past = now.minusDays(10);
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(past, now.plusDays(30)));
        AccountStatusRecord input = record(new PeriodRecord(past, now.plusDays(60)), null);

        assertDoesNotThrow(
            () -> validator.ensureValidityStartNotChangedWhenPast(current, input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityStartNotChangedWhenPast should be a no-op when persisted start is in the future")
    void testEnsureValidityStartNotChangedWhenPast_shouldSkipWhenPersistedInFuture() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.plusDays(1), now.plusDays(30)));
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(2), now.plusDays(60)), null);

        assertDoesNotThrow(
            () -> validator.ensureValidityStartNotChangedWhenPast(current, input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureNewValidityStartNotInPast should throw 400 when new start is in the past")
    void testEnsureNewValidityStartNotInPast_shouldThrowWhenInPast() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(new PeriodRecord(now.minusDays(1), now.plusDays(30)), null);

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureNewValidityStartNotInPast(current, input, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_start_in_past", ex.getError().key());
    }

    @Test
    @DisplayName("ensureNewValidityStartNotInPast should accept boundary equality (start == now)")
    void testEnsureNewValidityStartNotInPast_shouldAcceptBoundary() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(new PeriodRecord(now, now.plusDays(30)), null);

        assertDoesNotThrow(
            () -> validator.ensureNewValidityStartNotInPast(current, input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureNewValidityStartNotInPast should skip when persisted start is in the past")
    void testEnsureNewValidityStartNotInPast_shouldSkipWhenPersistedInPast() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.minusDays(10), now.plusDays(30)));
        AccountStatusRecord input = record(new PeriodRecord(now.minusDays(10), now.plusDays(30)), null);

        assertDoesNotThrow(
            () -> validator.ensureNewValidityStartNotInPast(current, input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityEndNotInPast should throw 400 when end is in the past")
    void testEnsureValidityEndNotInPast_shouldThrowWhenInPast() {
        AccountStatusRecord input = record(new PeriodRecord(now.minusDays(30), now.minusDays(1)), null);

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureValidityEndNotInPast(input, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_end_in_past", ex.getError().key());
    }

    @Test
    @DisplayName("ensureValidityEndNotInPast should accept boundary equality (end == now)")
    void testEnsureValidityEndNotInPast_shouldAcceptBoundary() {
        AccountStatusRecord input = record(new PeriodRecord(now.minusDays(30), now), null);

        assertDoesNotThrow(() -> validator.ensureValidityEndNotInPast(input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureValidityEndNotInPast should accept null end (open-ended)")
    void testEnsureValidityEndNotInPast_shouldAcceptNullEnd() {
        AccountStatusRecord input = record(new PeriodRecord(now.plusDays(1), null), null);

        assertDoesNotThrow(() -> validator.ensureValidityEndNotInPast(input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionStartAfterValidityStart should throw 400 when suspension start is before validity start")
    void testEnsureSuspensionStartAfterValidityStart_shouldThrowWhenBefore() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(5), now.plusDays(30)),
            new PeriodRecord(now.plusDays(1), now.plusDays(10))
        );

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionStartAfterValidityStart(current, input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_start_before_validity_start", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionStartAfterValidityStart should accept boundary equality")
    void testEnsureSuspensionStartAfterValidityStart_shouldAcceptBoundary() {
        OffsetDateTime aligned = now.plusDays(5);
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(aligned, now.plusDays(30)),
            new PeriodRecord(aligned, now.plusDays(10))
        );

        assertDoesNotThrow(() -> validator.ensureSuspensionStartAfterValidityStart(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionStartAfterValidityStart should fall back to persisted validity start when request omits it")
    void testEnsureSuspensionStartAfterValidityStart_shouldFallbackToPersisted() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.plusDays(10), now.plusDays(30)));
        AccountStatusRecord input = record(null, new PeriodRecord(now.plusDays(5), now.plusDays(20)));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionStartAfterValidityStart(current, input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_start_before_validity_start", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionStartNotInPast should throw 400 when suspension start is in the past")
    void testEnsureSuspensionStartNotInPast_shouldThrowWhenInPast() {
        AccountStatusRecord input = record(null, new PeriodRecord(now.minusDays(1), now.plusDays(10)));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionStartNotInPast(input, now, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_start_in_past", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionStartNotInPast should accept boundary equality (start == now)")
    void testEnsureSuspensionStartNotInPast_shouldAcceptBoundary() {
        AccountStatusRecord input = record(null, new PeriodRecord(now, now.plusDays(10)));

        assertDoesNotThrow(() -> validator.ensureSuspensionStartNotInPast(input, now, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionWithinValidity should throw 400 when suspension start is after validity end")
    void testEnsureSuspensionWithinValidity_shouldThrowWhenStartOutside() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(1), now.plusDays(10)),
            new PeriodRecord(now.plusDays(20), now.plusDays(25))
        );

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionWithinValidity(current, input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_outside_validity", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionWithinValidity should throw 400 when suspension end is after validity end")
    void testEnsureSuspensionWithinValidity_shouldThrowWhenEndOutside() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(1), now.plusDays(10)),
            new PeriodRecord(now.plusDays(5), now.plusDays(15))
        );

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionWithinValidity(current, input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_outside_validity", ex.getError().key());
    }

    @Test
    @DisplayName("ensureSuspensionWithinValidity should accept suspension within validity")
    void testEnsureSuspensionWithinValidity_shouldAcceptInside() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(1), now.plusDays(30)),
            new PeriodRecord(now.plusDays(5), now.plusDays(10))
        );

        assertDoesNotThrow(() -> validator.ensureSuspensionWithinValidity(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionWithinValidity should skip when validity end is null (open-ended)")
    void testEnsureSuspensionWithinValidity_shouldSkipWhenOpenEnded() {
        AccountStatus current = new AccountStatus();
        AccountStatusRecord input = record(
            new PeriodRecord(now.plusDays(1), null),
            new PeriodRecord(now.plusDays(5), now.plusDays(10))
        );

        assertDoesNotThrow(() -> validator.ensureSuspensionWithinValidity(current, input, ACCOUNT_ID));
    }

    @Test
    @DisplayName("ensureSuspensionWithinValidity should fall back to persisted validity end when request omits it")
    void testEnsureSuspensionWithinValidity_shouldFallbackToPersisted() {
        AccountStatus current = new AccountStatus();
        current.setValidityPeriod(closedOpen(now.plusDays(1), now.plusDays(10)));
        AccountStatusRecord input = record(null, new PeriodRecord(now.plusDays(20), now.plusDays(25)));

        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureSuspensionWithinValidity(current, input, ACCOUNT_ID));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_outside_validity", ex.getError().key());
    }
}
