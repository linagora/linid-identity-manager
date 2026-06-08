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

import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusRecord;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: OrganizationalUnitStatusValidator")
class OrganizationalUnitStatusValidatorTest {

    private static final UUID OU_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private OrganizationalUnitStatusValidator validator;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        this.validator = new OrganizationalUnitStatusValidator(new CommonMapper(), new PeriodValidator());
        this.now = OffsetDateTime.now();
    }

    private static OrganizationalUnitStatusRecord record(final PeriodRecord suspension) {
        return new OrganizationalUnitStatusRecord(suspension, null, null, null);
    }

    private static Range<ZonedDateTime> closedOpen(final OffsetDateTime start, final OffsetDateTime end) {
        return Range.closedOpen(start.toZonedDateTime(), end.toZonedDateTime());
    }

    @Test
    @DisplayName("validate should accept a coherent suspension period in the future")
    void testValidate_shouldAcceptCoherentFuturePeriod() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(now.plusDays(1), now.plusDays(10)));

        assertDoesNotThrow(() -> validator.validate(current, input, OU_ID));
    }

    @Test
    @DisplayName("validate should accept an open-ended (permanent) future suspension (end null)")
    void testValidate_shouldAcceptOpenEndedFuturePeriod() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(now.plusDays(1), null));

        assertDoesNotThrow(() -> validator.validate(current, input, OU_ID));
    }

    @Test
    @DisplayName("validate should reject a suspension period whose start is after its end")
    void testValidate_shouldRejectInvalidPeriod() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(now.plusDays(10), now.plusDays(1)));

        ApiException exception = assertThrows(ApiException.class,
            () -> validator.validate(current, input, OU_ID));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.status.suspension_period_invalid", exception.getError().key());
        assertEquals(OU_ID.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("validate should reject a suspension start in the past")
    void testValidate_shouldRejectStartInPast() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(now.minusDays(1), now.plusDays(10)));

        ApiException exception = assertThrows(ApiException.class,
            () -> validator.validate(current, input, OU_ID));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.status.suspension_start_in_past", exception.getError().key());
        assertEquals(OU_ID.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("validate should reject a suspension end in the past")
    void testValidate_shouldRejectEndInPast() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(null, now.minusDays(1)));

        ApiException exception = assertThrows(ApiException.class,
            () -> validator.validate(current, input, OU_ID));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.status.suspension_end_in_past", exception.getError().key());
        assertEquals(OU_ID.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("validate should accept an idempotent past suspension start (e.g. editing only the end date)")
    void testValidate_shouldAcceptIdempotentPastStart() {
        OffsetDateTime pastStart = now.minusDays(5);
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        current.setSuspensionPeriod(closedOpen(pastStart, now.plusDays(10)));
        // Same past start echoed back, only the end changes → idempotent → accepted
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(pastStart, now.plusDays(30)));

        assertDoesNotThrow(() -> validator.validate(current, input, OU_ID));
    }

    @Test
    @DisplayName("validate should reject a suspension start changed to a different past date")
    void testValidate_shouldRejectStartChangedToAnotherPastDate() {
        OrganizationalUnitStatus current = new OrganizationalUnitStatus();
        current.setSuspensionPeriod(closedOpen(now.minusDays(10), now.plusDays(10)));
        // Different past start → not idempotent → reject
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(now.minusDays(3), now.plusDays(10)));

        ApiException exception = assertThrows(ApiException.class,
            () -> validator.validate(current, input, OU_ID));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.status.suspension_start_in_past", exception.getError().key());
        assertEquals(OU_ID.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("ensureSuspensionPeriodCoherent should accept equal start and end bounds")
    void testEnsureSuspensionPeriodCoherent_shouldAcceptEqualBounds() {
        OffsetDateTime instant = now.plusDays(3);
        OrganizationalUnitStatusRecord input = record(new PeriodRecord(instant, instant));

        assertDoesNotThrow(() -> validator.ensureSuspensionPeriodCoherent(input, OU_ID));
    }
}
