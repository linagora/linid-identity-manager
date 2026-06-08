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

import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusRecord;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the business rules applied when updating an organizational unit suspension status
 * via {@code PUT /organizational-units/{id}/status}.
 *
 * <p>Each rule is exposed as a public {@code ensureXxx} method so it can be tested in isolation.
 * The {@link #validate(OrganizationalUnitStatus, OrganizationalUnitStatusRecord, UUID)} entry point
 * composes them. All rules
 * throw an {@link ApiException} with HTTP 400 when violated. A suspension always lies in the future:
 * both bounds, when provided, must be greater than or equal to now, and the start must be before or
 * equal to the end.</p>
 */
@Component
@RequiredArgsConstructor
public class OrganizationalUnitStatusValidator {

    /**
     * Shared mapper providing range and period accessor helpers, so that bound extraction logic
     * stays in one place.
     */
    private final CommonMapper commonMapper;

    /**
     * Reusable validator for generic temporal-period checks (e.g. {@code start <= end}).
     */
    private final PeriodValidator periodValidator;

    /**
     * Runs every status-update rule against the incoming request record.
     *
     * @param current              the persisted status, used to accept idempotent updates
     * @param record               the request record carrying the requested suspension fields
     * @param organizationalUnitId the organizational unit UUID, used to enrich error messages
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final OrganizationalUnitStatus current,
                         final OrganizationalUnitStatusRecord record,
                         final UUID organizationalUnitId) {
        OffsetDateTime now = OffsetDateTime.now();

        ensureSuspensionPeriodCoherent(record, organizationalUnitId);
        ensureSuspensionStartNotInPast(current, record, now, organizationalUnitId);
        ensureSuspensionEndNotInPast(record, now, organizationalUnitId);
    }

    /**
     * Ensures the requested suspension period is internally coherent ({@code start <= end}) when
     * both bounds are provided. Open-ended ranges (one bound {@code null}) skip this check.
     *
     * @param record               the request record
     * @param organizationalUnitId the organizational unit UUID, used in the error message
     * @throws ApiException with key
     *     {@code error.organizational.unit.status.suspension_period_invalid} (HTTP 400)
     */
    public void ensureSuspensionPeriodCoherent(final OrganizationalUnitStatusRecord record,
                                               final UUID organizationalUnitId) {
        periodValidator.ensureCoherent(commonMapper.startOf(record.suspensionPeriod()),
            commonMapper.endOf(record.suspensionPeriod()),
            "error.organizational.unit.status.suspension_period_invalid",
            Map.of("id", organizationalUnitId.toString()));
    }

    /**
     * Rejects a suspension start strictly in the past. {@code null} starts are accepted.
     * Idempotent calls (requested suspension start equals the persisted suspension start) are
     * accepted regardless of whether the start is in the past, so editing an ongoing suspension
     * (e.g. updating only its end date) does not fail.
     *
     * @param current              the persisted status, used to detect idempotent updates
     * @param record               the request record
     * @param now                  the current instant
     * @param organizationalUnitId the organizational unit UUID, used in the error message
     * @throws ApiException with key
     *     {@code error.organizational.unit.status.suspension_start_in_past} (HTTP 400)
     */
    public void ensureSuspensionStartNotInPast(final OrganizationalUnitStatus current,
                                               final OrganizationalUnitStatusRecord record,
                                               final OffsetDateTime now,
                                               final UUID organizationalUnitId) {
        OffsetDateTime suspensionStart = commonMapper.startOf(record.suspensionPeriod());

        if (suspensionStart == null) {
            return;
        }

        OffsetDateTime persistedSuspensionStart = commonMapper.startOf(current.getSuspensionPeriod());
        if (persistedSuspensionStart != null && suspensionStart.isEqual(persistedSuspensionStart)) {
            return;
        }

        if (suspensionStart.isBefore(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.status.suspension_start_in_past",
                    Map.of("id", organizationalUnitId.toString(),
                        "start", suspensionStart.toString()))
            );
        }
    }

    /**
     * Rejects a suspension end strictly in the past. {@code null} ends (open-ended / permanent
     * suspension) are accepted.
     *
     * @param record               the request record
     * @param now                  the current instant
     * @param organizationalUnitId the organizational unit UUID, used in the error message
     * @throws ApiException with key
     *     {@code error.organizational.unit.status.suspension_end_in_past} (HTTP 400)
     */
    public void ensureSuspensionEndNotInPast(final OrganizationalUnitStatusRecord record,
                                             final OffsetDateTime now,
                                             final UUID organizationalUnitId) {
        OffsetDateTime suspensionEnd = commonMapper.endOf(record.suspensionPeriod());

        if (suspensionEnd != null && suspensionEnd.isBefore(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.status.suspension_end_in_past",
                    Map.of("id", organizationalUnitId.toString(),
                        "end", suspensionEnd.toString()))
            );
        }
    }
}
