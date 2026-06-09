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

import io.github.linagora.linid.im.api.model.account.AccountValidityRecord;
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
 * Validates a {@code PUT /accounts/{id}/status/validity} request, which schedules the validity
 * period start of an account.
 */
@Component
@RequiredArgsConstructor
public class AccountValidityValidator {

    /**
     * Shared mapper providing range and period accessor helpers.
     */
    private final CommonMapper commonMapper;

    /**
     * Reusable validator for generic temporal-period checks.
     */
    private final PeriodValidator periodValidator;

    /**
     * Runs every validity rule against the persisted state and the incoming request record.
     *
     * @param current   the persisted {@link AccountStatus} of the targeted account
     * @param record    the validity request record
     * @param accountId the account UUID, used to enrich error messages
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final AccountStatus current,
                         final AccountValidityRecord record,
                         final UUID accountId) {
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> context = Map.of("id", accountId.toString());

        ensureNotAlreadyActivated(current, accountId);
        ensureValidityStartInFuture(record, now, accountId);
        periodValidator.ensureCoherent(record.validityStart(),
            commonMapper.endOf(current.getValidityPeriod()),
            "error.account.status.validity_start_after_end", context);
    }

    /**
     * Rejects scheduling a new validity start once the account has been activated. After activation
     * the validity period start can never be changed; only its end may be updated (via
     * deactivation).
     *
     * @param current   the persisted account status
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_start_locked} (HTTP 400)
     *     when the account is already activated
     */
    public void ensureNotAlreadyActivated(final AccountStatus current, final UUID accountId) {
        if (current.getActivationAt() != null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_start_locked",
                    Map.of("id", accountId.toString()))
            );
        }
    }

    /**
     * Rejects a validity start that is not strictly in the future.
     *
     * @param record    the request record
     * @param now       the current instant
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.validity_start_not_in_future} (HTTP 400)
     */
    public void ensureValidityStartInFuture(final AccountValidityRecord record,
                                            final OffsetDateTime now,
                                            final UUID accountId) {
        if (!record.validityStart().isAfter(now)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.validity_start_not_in_future",
                    Map.of("id", accountId.toString(), "start", record.validityStart().toString()))
            );
        }
    }
}
