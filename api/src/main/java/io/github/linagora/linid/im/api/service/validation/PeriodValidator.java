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

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reusable temporal-period validation rules. Centralizes generic checks that can apply to any
 * period (validity, suspension, etc.) so each domain validator (e.g.
 * {@link AccountSuspensionValidator}) can compose them rather than duplicating the logic.
 */
@Component
public class PeriodValidator {

    /**
     * Asserts {@code start <= end} when both bounds are non-null. {@code null} bounds (open-ended
     * periods) skip the check. The raised error message exposes the {@code start} and {@code end}
     * values; callers needing extra context (e.g. an entity id) should use the overload accepting
     * an initial context map.
     *
     * @param start    the period start (may be {@code null})
     * @param end      the period end (may be {@code null})
     * @param errorKey the i18n error key to raise on violation
     * @throws ApiException with the given i18n key (HTTP 400) when start is strictly after end
     */
    public void ensureCoherent(final OffsetDateTime start,
                               final OffsetDateTime end,
                               final String errorKey) {
        ensureCoherent(start, end, errorKey, Map.of());
    }

    /**
     * Asserts {@code start <= end} when both bounds are non-null. {@code null} bounds (open-ended
     * periods) skip the check. The {@code initialContext} entries are forwarded into the i18n
     * message in addition to the {@code start} and {@code end} values, so callers can attach
     * domain-specific parameters (e.g. an entity id).
     *
     * @param start          the period start (may be {@code null})
     * @param end            the period end (may be {@code null})
     * @param errorKey       the i18n error key to raise on violation
     * @param initialContext extra entries to expose in the i18n message
     * @throws ApiException with the given i18n key (HTTP 400) when start is strictly after end
     */
    public void ensureCoherent(final OffsetDateTime start,
                               final OffsetDateTime end,
                               final String errorKey,
                               final Map<String, Object> initialContext) {
        if (start == null || end == null) {
            return;
        }
        if (start.isAfter(end)) {
            Map<String, Object> params = new HashMap<>(initialContext);
            params.put("start", start.toString());
            params.put("end", end.toString());
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(errorKey, params)
            );
        }
    }

    /**
     * Rejects a period start strictly before {@code now}. A {@code null} start (open-ended on the
     * lower bound) is accepted. Idempotent updates are accepted: when the requested {@code start}
     * equals {@code persistedStart}, the check is skipped so editing an ongoing period (e.g. only
     * its end date) does not fail because its start now lies in the past.
     *
     * @param start          the requested period start (may be {@code null})
     * @param persistedStart the currently persisted start, used to detect idempotent updates
     * @param now            the current instant
     * @param errorKey       the i18n error key to raise on violation
     * @param initialContext extra entries to expose in the i18n message
     * @throws ApiException with the given i18n key (HTTP 400) when start is strictly in the past
     */
    public void ensureStartNotInPast(final OffsetDateTime start,
                                     final OffsetDateTime persistedStart,
                                     final OffsetDateTime now,
                                     final String errorKey,
                                     final Map<String, Object> initialContext) {
        if (start == null) {
            return;
        }
        if (persistedStart != null && start.isEqual(persistedStart)) {
            return;
        }
        if (start.isBefore(now)) {
            Map<String, Object> params = new HashMap<>(initialContext);
            params.put("start", start.toString());
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(errorKey, params)
            );
        }
    }

    /**
     * Rejects a period end strictly before {@code now}. A {@code null} end (open-ended on the upper
     * bound) is accepted.
     *
     * @param end            the requested period end (may be {@code null})
     * @param now            the current instant
     * @param errorKey       the i18n error key to raise on violation
     * @param initialContext extra entries to expose in the i18n message
     * @throws ApiException with the given i18n key (HTTP 400) when end is strictly in the past
     */
    public void ensureEndNotInPast(final OffsetDateTime end,
                                   final OffsetDateTime now,
                                   final String errorKey,
                                   final Map<String, Object> initialContext) {
        if (end != null && end.isBefore(now)) {
            Map<String, Object> params = new HashMap<>(initialContext);
            params.put("end", end.toString());
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(errorKey, params)
            );
        }
    }
}
