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

import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Shared mapper bridging the API contract ({@link PeriodDTO}, {@link PeriodRecord}) and the
 * persistence representation of a PostgreSQL {@code TSTZRANGE} as
 * {@link Range Range&lt;ZonedDateTime&gt;}.
 *
 * <p>The API exposes {@link OffsetDateTime} bounds (matching the JSON contract consumed by the
 * frontend) while the persistence layer relies on {@link ZonedDateTime} bounds, as required by
 * {@link io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType}. This mapper centralizes
 * the conversion in a single place so consumers never deal with two different representations.</p>
 */
@Component
public class CommonMapper {

    /**
     * Converts a {@link PeriodRecord} from an incoming request into a persistence
     * {@link Range Range&lt;ZonedDateTime&gt;}.
     *
     * <p>Returns {@code null} when both bounds are {@code null} (no range to persist). Open-ended
     * ranges (one bound {@code null}) are mapped to half-infinite ranges. Bounded ranges follow the
     * standard PostgreSQL convention {@code [start, end)} (inclusive lower, exclusive upper).</p>
     *
     * @param period the request record, possibly {@code null}
     * @return a {@link Range Range&lt;ZonedDateTime&gt;} reflecting the input bounds, or
     *         {@code null} when the input is {@code null} or has both bounds unset
     */
    public Range<ZonedDateTime> toRange(final PeriodRecord period) {
        if (period == null) {
            return null;
        }
        ZonedDateTime start = toZonedDateTime(period.start());
        ZonedDateTime end = toZonedDateTime(period.end());
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            return Range.infiniteOpen(end);
        }
        if (end == null) {
            return Range.closedInfinite(start);
        }
        return Range.closedOpen(start, end);
    }

    /**
     * Converts a persistence {@link Range Range&lt;ZonedDateTime&gt;} into a {@link PeriodDTO} for
     * API responses.
     *
     * @param range the persisted range, possibly {@code null}
     * @return a {@link PeriodDTO} exposing both bounds as {@link OffsetDateTime}, or {@code null}
     *         when the input is {@code null}
     */
    public PeriodDTO toPeriodDTO(final Range<ZonedDateTime> range) {
        if (range == null) {
            return null;
        }
        OffsetDateTime start = null;
        if (range.hasLowerBound()) {
            start = toOffsetDateTime(range.lower());
        }
        OffsetDateTime end = null;
        if (range.hasUpperBound()) {
            end = toOffsetDateTime(range.upper());
        }
        return new PeriodDTO(start, end);
    }

    /**
     * Converts an {@link OffsetDateTime} (API contract) to a {@link ZonedDateTime} (persistence contract).
     *
     * @param value the value to convert, possibly {@code null}
     * @return the converted {@link ZonedDateTime}, or {@code null} when the input is {@code null}
     */
    private static ZonedDateTime toZonedDateTime(final OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toZonedDateTime();
    }

    /**
     * Converts a {@link ZonedDateTime} (persistence contract) to an {@link OffsetDateTime} (API contract).
     *
     * @param value the value to convert, possibly {@code null}
     * @return the converted {@link OffsetDateTime}, or {@code null} when the input is {@code null}
     */
    private static OffsetDateTime toOffsetDateTime(final ZonedDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
