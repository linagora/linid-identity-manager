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

import io.github.linagora.linid.im.api.model.account.AccountReactivationRecord;
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
 * Validates a {@code PUT /accounts/{id}/status/reactivate} request.
 *
 * <p>The mandatory justification comment is enforced by {@code @NotBlank} on
 * {@link AccountReactivationRecord}. This validator ensures there is something to reactivate:
 * either an active suspension to lift, or a deactivated account (validity period end in the past)
 * to re-validate. When a new {@code validityEnd} is provided, it must not be in the past.</p>
 */
@Component
@RequiredArgsConstructor
public class AccountReactivationValidator {

    /**
     * Shared mapper providing range and period accessor helpers.
     */
    private final CommonMapper commonMapper;

    /**
     * Reusable validator for generic temporal-period checks.
     */
    private final PeriodValidator periodValidator;

    /**
     * Runs every reactivation rule: the account must have something to reactivate (an active
     * suspension or an expired validity), and any provided new validity end must not be in the past.
     *
     * @param current   the persisted {@link AccountStatus} of the targeted account
     * @param record    the reactivation request record
     * @param accountId the account UUID, used to enrich error messages
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final AccountStatus current,
                         final AccountReactivationRecord record,
                         final UUID accountId) {
        ensureReactivatable(current, accountId);
        periodValidator.ensureEndNotInPast(record.validityEnd(), OffsetDateTime.now(),
            "error.account.status.validity_end_in_past", Map.of("id", accountId.toString()));
    }

    /**
     * Rejects reactivation when there is nothing to reactivate, i.e. the account is neither currently
     * suspended (an active suspension period) nor deactivated (a validity period end in the past).
     *
     * @param current   the persisted account status
     * @param accountId the account UUID, used in the error message
     * @throws ApiException with key {@code error.account.status.nothing_to_reactivate} (HTTP 400)
     */
    public void ensureReactivatable(final AccountStatus current, final UUID accountId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime suspensionEnd = commonMapper.endOf(current.getSuspensionPeriod());
        boolean suspended = current.getSuspensionPeriod() != null
            && (suspensionEnd == null || !suspensionEnd.isBefore(now));

        OffsetDateTime validityEnd = commonMapper.endOf(current.getValidityPeriod());
        boolean deactivated = validityEnd != null && validityEnd.isBefore(now);

        if (!suspended && !deactivated) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.status.nothing_to_reactivate",
                    Map.of("id", accountId.toString()))
            );
        }
    }
}
