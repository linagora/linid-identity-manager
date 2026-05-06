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

import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the business rules applied when creating an account via
 * {@code POST /accounts}.
 *
 * <p>Each rule is exposed as a public {@code ensureXxx} method so it can be tested in isolation.
 * The {@link #validate(AccountRecord)} entry point composes them in the order required by the issue.</p>
 */
@Component
@RequiredArgsConstructor
public class AccountCreationValidator {

    /**
     * Shared mapper providing range and period accessor helpers, so that bound extraction logic
     * stays in one place.
     */
    private final CommonMapper commonMapper;

    /**
     * Runs every creation rule against the given account record.
     *
     * @param account the account creation record to validate
     * @throws ApiException if any rule is violated (HTTP 400)
     */
    public void validate(final AccountRecord account) {
        OffsetDateTime validityPeriodStart = commonMapper.startOf(account.validityPeriod());
        String email = account.email();
        ensureValidityPeriodStartNotNull(validityPeriodStart, email);
        ensureValidityPeriodStartNotInPast(validityPeriodStart, email);
    }

    /**
     * Rejects account creation when the validity period is missing.
     *
     * @param validityPeriodStart the requested validity period start to validate
     * @param email               the account email, used in the error message
     * @throws ApiException with key {@code error.account.creation.validity_period_start_required} (HTTP 400)
     */
    public void ensureValidityPeriodStartNotNull(final OffsetDateTime validityPeriodStart, final String email) {
        if (validityPeriodStart == null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.creation.validity_period_start_required", Map.of("email", email))
            );
        }
    }

    /**
     * Rejects account creation when the validity period starts in the past.
     *
     * @param validityPeriodStart the requested validity period start to validate
     * @param email               the account email, used in the error message
     * @throws ApiException with key {@code error.account.creation.validity_period_start_in_past} (HTTP 400)
     */
    public void ensureValidityPeriodStartNotInPast(final OffsetDateTime validityPeriodStart, final String email) {
        if (validityPeriodStart != null && validityPeriodStart.isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.account.creation.validity_period_start_in_past",
                    Map.of("email", email, "start", validityPeriodStart.toString()))
            );
        }
    }
}
