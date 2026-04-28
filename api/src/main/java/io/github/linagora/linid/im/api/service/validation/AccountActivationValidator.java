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

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the business rules applied when activating an account via
 * {@code PUT /accounts/{id}/status/activation}.
 *
 * <p>Each rule is exposed as a public {@code ensureXxx} method so it can be tested in isolation.
 * The {@link #validate(AccountStatus, AccountActivationRecord, UUID)} entry point composes them
 * in the order required by the issue.</p>
 */
@Component
public class AccountActivationValidator {

    /**
     * Runs every activation rule against the given status and request record.
     *
     * @param status    the persisted {@link AccountStatus} of the targeted account
     * @param record    the request record carrying the requested {@code activationAt}
     * @param accountId the account UUID, used to enrich error messages
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final AccountStatus status,
                         final AccountActivationRecord record,
                         final UUID accountId) {
        ensureNotAlreadyActivated(status, accountId);
        OffsetDateTime validityStart = ensureValidityStartExists(status, accountId);
        OffsetDateTime now = OffsetDateTime.now();
        ensureValidityNotInFuture(validityStart, now, accountId);
        ensureActivationAfterStart(record, validityStart, accountId);
        ensureActivationNotInFuture(record, now, accountId);
    }

    /**
     * Rejects activation when the account is already activated.
     *
     * @param status    the persisted {@link AccountStatus}
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.activation.already_activated} (HTTP 400)
     */
    public void ensureNotAlreadyActivated(final AccountStatus status, final UUID accountId) {
        if (status.getActivationAt() != null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation.already_activated",
                    Map.of("id", accountId.toString()))
            );
        }
    }

    /**
     * Ensures the account has a validity period with a defined lower bound, returning that bound
     * as an {@link OffsetDateTime} for downstream comparisons.
     *
     * @param status    the persisted {@link AccountStatus}
     * @param accountId the account UUID, used in the error message
     * @return the validity period's lower bound as an {@link OffsetDateTime}
     * @throws ApiException with key {@code error.account.status.activation.validity_required} (HTTP 400)
     */
    public OffsetDateTime ensureValidityStartExists(final AccountStatus status, final UUID accountId) {
        Range<ZonedDateTime> validity = status.getValidityPeriod();
        if (validity == null || !validity.hasLowerBound()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation.validity_required",
                    Map.of("id", accountId.toString()))
            );
        }
        return validity.lower().toOffsetDateTime();
    }

    /**
     * Rejects activation when the validity period start is in the future relative to {@code now}.
     *
     * @param validityStart the validity period's lower bound
     * @param now           the current instant
     * @param accountId     the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.activation.validity_in_future} (HTTP 400)
     */
    public void ensureValidityNotInFuture(final OffsetDateTime validityStart, final OffsetDateTime now,
                                          final UUID accountId) {
        if (validityStart.isAfter(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation.validity_in_future",
                    Map.of("id", accountId.toString(),
                        "start", validityStart.toString()))
            );
        }
    }

    /**
     * Rejects activation when the requested {@code activationAt} is before the validity start.
     *
     * @param record        the activation request record
     * @param validityStart the validity period's lower bound
     * @param accountId     the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.activation.before_validity_start} (HTTP 400)
     */
    public void ensureActivationAfterStart(final AccountActivationRecord record,
                                           final OffsetDateTime validityStart,
                                           final UUID accountId) {
        if (record.activationAt().isBefore(validityStart)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation.before_validity_start",
                    Map.of("id", accountId.toString(),
                        "activationAt", record.activationAt().toString(),
                        "start", validityStart.toString()))
            );
        }
    }

    /**
     * Rejects activation when the requested {@code activationAt} is in the future relative to {@code now}.
     *
     * @param record    the activation request record
     * @param now       the current instant
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.activation.in_future} (HTTP 400)
     */
    public void ensureActivationNotInFuture(final AccountActivationRecord record,
                                            final OffsetDateTime now,
                                            final UUID accountId) {
        if (record.activationAt().isAfter(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.activation.in_future",
                    Map.of("id", accountId.toString(),
                        "activationAt", record.activationAt().toString()))
            );
        }
    }
}
