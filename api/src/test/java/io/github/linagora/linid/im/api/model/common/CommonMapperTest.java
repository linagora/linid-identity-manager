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

package io.github.linagora.linid.im.api.model.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: CommonMapper")
class CommonMapperTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private final CommonMapper mapper = new CommonMapper();

    @Test
    @DisplayName("toRange should return null on null record")
    void testToRange_shouldReturnNullOnNullRecord() {
        assertNull(mapper.toRange(null));
    }

    @Test
    @DisplayName("toRange should return null when both bounds are null")
    void testToRange_shouldReturnNullWhenBothBoundsNull() {
        assertNull(mapper.toRange(new PeriodRecord(null, null)));
    }

    @Test
    @DisplayName("toRange should produce a closed-open range when both bounds are set")
    void testToRange_shouldProduceClosedOpenRangeForBoundedInput() {
        Range<ZonedDateTime> range = mapper.toRange(new PeriodRecord(START, END));

        assertNotNull(range);
        assertTrue(range.hasLowerBound());
        assertTrue(range.hasUpperBound());
        assertTrue(range.isLowerBoundClosed());
        assertFalse(range.isUpperBoundClosed());
        assertEquals(START.toZonedDateTime(), range.lower());
        assertEquals(END.toZonedDateTime(), range.upper());
    }

    @Test
    @DisplayName("toRange should produce a half-infinite range when only end is set")
    void testToRange_shouldProduceInfiniteOpenWhenStartIsNull() {
        Range<ZonedDateTime> range = mapper.toRange(new PeriodRecord(null, END));

        assertNotNull(range);
        assertFalse(range.hasLowerBound());
        assertTrue(range.hasUpperBound());
        assertEquals(END.toZonedDateTime(), range.upper());
    }

    @Test
    @DisplayName("toRange should produce a half-infinite range when only start is set")
    void testToRange_shouldProduceClosedInfiniteWhenEndIsNull() {
        Range<ZonedDateTime> range = mapper.toRange(new PeriodRecord(START, null));

        assertNotNull(range);
        assertTrue(range.hasLowerBound());
        assertFalse(range.hasUpperBound());
        assertEquals(START.toZonedDateTime(), range.lower());
    }

    @Test
    @DisplayName("toPeriodDTO should return null on null range")
    void testToPeriodDTO_shouldReturnNullOnNullRange() {
        assertNull(mapper.toPeriodDTO(null));
    }

    @Test
    @DisplayName("toPeriodDTO should expose both bounds when defined")
    void testToPeriodDTO_shouldExposeBothBounds() {
        Range<ZonedDateTime> range = Range.closedOpen(START.toZonedDateTime(), END.toZonedDateTime());

        PeriodDTO dto = mapper.toPeriodDTO(range);

        assertNotNull(dto);
        assertEquals(START, dto.getStart());
        assertEquals(END, dto.getEnd());
    }

    @Test
    @DisplayName("toPeriodDTO should expose null bounds when the range is half-infinite")
    void testToPeriodDTO_shouldExposeNullBoundsWhenHalfInfinite() {
        PeriodDTO startOnly = mapper.toPeriodDTO(Range.closedInfinite(START.toZonedDateTime()));
        assertNotNull(startOnly);
        assertEquals(START, startOnly.getStart());
        assertNull(startOnly.getEnd());

        PeriodDTO endOnly = mapper.toPeriodDTO(Range.infiniteOpen(END.toZonedDateTime()));
        assertNotNull(endOnly);
        assertNull(endOnly.getStart());
        assertEquals(END, endOnly.getEnd());
    }
}
