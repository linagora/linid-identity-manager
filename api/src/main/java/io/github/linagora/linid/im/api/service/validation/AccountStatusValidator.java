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

import io.github.linagora.linid.im.api.model.account.AccountStatusRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the business rules applied when updating an account status via
 * {@code PUT /accounts/{id}/status}.
 *
 * <p>Each rule is exposed as a public {@code ensureXxx} method so it can be tested in isolation.
 * The {@link #validate(AccountStatus, AccountStatusRecord, UUID)} entry point composes them in
 * the order required by the issue. All rules throw an {@link ApiException} with HTTP 400 when
 * violated; {@code null} request bounds remain valid (open-ended periods) unless the rule
 * explicitly checks them.</p>
 */
@Component
@RequiredArgsConstructor
public class AccountStatusValidator {

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
     * Runs every status-update rule against the persisted state and the incoming request record.
     *
     * <p><b>PUT semantics:</b> the endpoint follows replace semantics, meaning the caller must
     * echo every field they want to keep (notably {@code validityPeriod} when only modifying
     * suspension or status reason). Suspension rules cross-checked against the validity period
     * fall back to the persisted {@code validityPeriod} when the request omits it, so a partial
     * PUT touching only suspension still respects the persisted validity bounds.</p>
     *
     * @param current   the persisted {@link AccountStatus} of the targeted account; the caller
     *                  guarantees the row exists (via {@code orElseThrow}), and the DB constraint {@code
     *                  chk_account_status_validity_has_lower_bound} guarantees that {@code validityPeriod} carries
     *                  a finite lower bound
     * @param record    the request record carrying the requested status fields
     * @param accountId the account UUID, used to enrich error messages
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final AccountStatus current,
                         final AccountStatusRecord record,
                         final UUID accountId) {
        OffsetDateTime now = OffsetDateTime.now();

        ensureActivationAtNotProvided(record, accountId);
        ensureValidityPeriodStartNotNull(record, accountId);
        ensureValidityPeriodCoherent(record, accountId);
        ensureSuspensionPeriodCoherent(record, accountId);
        ensureValidityStartNotChangedWhenPast(current, record, now, accountId);
        ensureNewValidityStartNotInPast(current, record, now, accountId);
        ensureValidityEndNotInPast(record, now, accountId);
        ensureSuspensionStartAfterValidityStart(record, accountId);
        ensureSuspensionStartNotInPast(current, record, now, accountId);
        ensureSuspensionWithinValidity(record, accountId);
    }

    /**
     * Rejects requests that try to set {@code activationAt} on this endpoint. Activation is
     * managed exclusively by {@code PUT /accounts/{id}/status/activation}.
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.activation_at_read_only} (HTTP 400)
     */
    public void ensureActivationAtNotProvided(final AccountStatusRecord record, final UUID accountId) {
        if (record.activationAt() != null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation_at_read_only",
                    Map.of("id", accountId.toString()))
            );
        }
    }

    /**
     * Rejects requests whose {@code validityPeriod} has no lower bound (period absent or open-ended
     * start). A finite start is mandatory for every status update — it mirrors the DB constraint
     * {@code chk_account_status_validity_has_lower_bound} on the persisted row.
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_period_start_required} (HTTP
     *                      400)
     */
    public void ensureValidityPeriodStartNotNull(
        final AccountStatusRecord record, final UUID accountId) {
        OffsetDateTime requestedStart = commonMapper.startOf(record.validityPeriod());

        if (requestedStart == null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_period_start_required",
                    Map.of("id", accountId.toString()))
            );
        }
    }

    /**
     * Ensures the requested validity period is internally coherent ({@code start <= end}) when both
     * bounds are provided. Open-ended ranges (one bound {@code null}) skip this check.
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_period_invalid} (HTTP 400)
     */
    public void ensureValidityPeriodCoherent(final AccountStatusRecord record, final UUID accountId) {
        periodValidator.ensureCoherent(commonMapper.startOf(record.validityPeriod()),
            commonMapper.endOf(record.validityPeriod()),
            "error.account.status.validity_period_invalid",
            Map.of("id", accountId.toString()));
    }

    /**
     * Ensures the requested suspension period is internally coherent ({@code start <= end}) when
     * both bounds are provided. Open-ended ranges (one bound {@code null}) skip this check.
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.suspension_period_invalid} (HTTP 400)
     */
    public void ensureSuspensionPeriodCoherent(final AccountStatusRecord record, final UUID accountId) {
        periodValidator.ensureCoherent(commonMapper.startOf(record.suspensionPeriod()),
            commonMapper.endOf(record.suspensionPeriod()),
            "error.account.status.suspension_period_invalid",
            Map.of("id", accountId.toString()));
    }

    /**
     * Rejects updates that change a validity start which is already in the past. Idempotent calls
     * (request start equals persisted start) are accepted. Both {@code persistedStart} and {@code
     * requestedStart} are guaranteed non-{@code null} by upstream invariants: the DB constraint on
     * the persisted row, and {@link #ensureValidityPeriodStartNotNull} on the request.
     *
     * @param current   the persisted {@link AccountStatus}
     * @param record    the request record
     * @param now       the current instant
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_start_frozen} (HTTP 400)
     */
    public void ensureValidityStartNotChangedWhenPast(final AccountStatus current,
                                                      final AccountStatusRecord record,
                                                      final OffsetDateTime now,
                                                      final UUID accountId) {
        OffsetDateTime persistedStart = commonMapper.startOf(current.getValidityPeriod());
        OffsetDateTime requestedStart = commonMapper.startOf(record.validityPeriod());

        if (persistedStart.isEqual(requestedStart)) {
            return;
        }

        if (persistedStart.isBefore(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_start_frozen",
                    Map.of("id", accountId.toString(),
                        "current", persistedStart.toString()))
            );
        }
    }

    /**
     * Rejects a new validity start that is in the past, when the persisted start is in
     * the future. The freeze rule (see
     * {@link #ensureValidityStartNotChangedWhenPast(AccountStatus, AccountStatusRecord, OffsetDateTime, UUID)})
     * already protects past persisted starts from any change, so this rule only applies to the
     * "first activation or future-scheduled" case.
     *
     * @param current   the persisted {@link AccountStatus}
     * @param record    the request record
     * @param now       the current instant
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_start_in_past} (HTTP 400)
     */
    public void ensureNewValidityStartNotInPast(final AccountStatus current,
                                                final AccountStatusRecord record,
                                                final OffsetDateTime now,
                                                final UUID accountId) {
        OffsetDateTime persistedStart = commonMapper.startOf(current.getValidityPeriod());
        OffsetDateTime requestedStart = commonMapper.startOf(record.validityPeriod());

        if (persistedStart.isEqual(requestedStart)) {
            return;
        }

        if (requestedStart.isBefore(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_start_in_past",
                    Map.of("id", accountId.toString(),
                        "start", requestedStart.toString()))
            );
        }
    }

    /**
     * Rejects a validity end strictly in the past. {@code null} ends are accepted (no expiry).
     *
     * @param record    the request record
     * @param now       the current instant
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_end_in_past} (HTTP 400)
     */
    public void ensureValidityEndNotInPast(final AccountStatusRecord record,
                                           final OffsetDateTime now,
                                           final UUID accountId) {
        OffsetDateTime requestedEnd = commonMapper.endOf(record.validityPeriod());

        if (requestedEnd != null && requestedEnd.isBefore(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_end_in_past",
                    Map.of("id", accountId.toString(),
                        "end", requestedEnd.toString()))
            );
        }
    }

    /**
     * Rejects a suspension start strictly before the validity start. Both bounds are read from the
     * request — {@code validityPeriod.start} is guaranteed non-{@code null} by {@link
     * #ensureValidityPeriodStartNotNull} upstream. Skipped when no suspension start is provided.
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.suspension_start_before_validity_start} (HTTP 400)
     */
    public void ensureSuspensionStartAfterValidityStart(final AccountStatusRecord record,
                                                        final UUID accountId) {
        OffsetDateTime suspensionStart = commonMapper.startOf(record.suspensionPeriod());
        OffsetDateTime validityStart = commonMapper.startOf(record.validityPeriod());

        if (suspensionStart != null && suspensionStart.isBefore(validityStart)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.suspension_start_before_validity_start",
                    Map.of("id", accountId.toString(),
                        "suspensionStart", suspensionStart.toString(),
                        "validityStart", validityStart.toString()))
            );
        }
    }

    /**
     * Rejects a suspension start strictly in the past. {@code null} suspension starts are accepted.
     * Idempotent calls (requested suspension start equals the persisted suspension start) are
     * accepted regardless of whether the start is in the past.
     *
     * @param current   the persisted {@link AccountStatus}
     * @param record    the request record
     * @param now       the current instant (start of day)
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.suspension_start_in_past} (HTTP 400)
     */
    public void ensureSuspensionStartNotInPast(final AccountStatus current,
                                               final AccountStatusRecord record,
                                               final OffsetDateTime now,
                                               final UUID accountId) {
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
                I18nMessage.of("error.account.status.suspension_start_in_past",
                    Map.of("id", accountId.toString(),
                        "start", suspensionStart.toString()))
            );
        }
    }

    /**
     * Rejects suspension bounds that fall outside the validity end. Both {@code
     * suspensionPeriod.start} and {@code suspensionPeriod.end} (when present) must be {@code <=
     * validityPeriod.end}. Skipped when {@code validityPeriod.end} is {@code null} (open-ended
     * validity).
     *
     * @param record    the request record
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.suspension_outside_validity} (HTTP 400)
     */
    public void ensureSuspensionWithinValidity(final AccountStatusRecord record,
                                               final UUID accountId) {
        OffsetDateTime validityEnd = commonMapper.endOf(record.validityPeriod());

        if (validityEnd == null) {
            return;
        }

        OffsetDateTime suspensionStart = commonMapper.startOf(record.suspensionPeriod());
        OffsetDateTime suspensionEnd = commonMapper.endOf(record.suspensionPeriod());
        boolean startOutside = suspensionStart != null && suspensionStart.isAfter(validityEnd);
        boolean endOutside = suspensionEnd != null && suspensionEnd.isAfter(validityEnd);

        if (startOutside || endOutside) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.suspension_outside_validity",
                    Map.of("id", accountId.toString(),
                        "validityEnd", validityEnd.toString()))
            );
        }
    }

}
