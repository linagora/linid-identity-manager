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

package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountStatusRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for account management operations.
 */
public interface AccountService {

    /**
     * Creates a new account from the given request.
     *
     * <p> Business rules enforced (delegated to {@code AccountCreationValidator}):</p>
     * <ul>
     *   <li>The validity period must exist with a non-null start.</li>
     *   <li>The validity period's start must be greater than or equal to {@code now()}.</li>
     * </ul>
     *
     * @param userPrincipal the authenticated user
     * @param account       the account creation record
     * @return the created account entity
     */
    Account create(UserPrincipal userPrincipal, AccountRecord account);

    /**
     * Retrieves a paginated list of accounts, optionally filtered.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination information
     * @return a page of account entities
     */
    Page<AccountView> findAll(UserPrincipal userPrincipal, AccountViewQueryFilterDto filters, Pageable pageable);

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @return the account entity
     * @throws io.github.linagora.linid.im.corelib.exception.ApiException if not found
     */
    AccountView findById(UserPrincipal userPrincipal, UUID id);

    /**
     * Deletes an account by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @throws io.github.linagora.linid.im.corelib.exception.ApiException if not found
     */
    void deleteById(UserPrincipal userPrincipal, UUID id);

    /**
     * Retrieves an {@link Account} associated with the given email address.
     *
     * @param email the email address used to look up the account
     * @return an {@link Optional} containing the matching {@link Account} if found,
     * or {@link Optional#empty()} if no account exists for the given email
     */
    Optional<Account> getAccountByEmail(String email);

    /**
     * Updates the status fields of the account with the given identifier, after enforcing the
     * business rules delegated to {@code AccountStatusValidator}. Validated fields are then
     * persisted as-is.
     *
     * <p>Business rules enforced (delegated to {@code AccountStatusValidator}):</p>
     * <ul>
     *   <li>{@code activationAt} must be {@code null} (managed exclusively by
     *       {@code PUT /accounts/{id}/status/activation}).</li>
     *   <li>The validity period must carry a finite lower bound ({@code validityPeriod.start} is
     *       mandatory).</li>
     *   <li>The validity period must be internally coherent: when both bounds are provided,
     *       {@code validityPeriod.start <= validityPeriod.end}.</li>
     *   <li>The suspension period must be internally coherent: when both bounds are provided,
     *       {@code suspensionPeriod.start <= suspensionPeriod.end}.</li>
     *   <li>If the persisted {@code validityPeriod.start} is already in the past, it cannot be
     *       changed (frozen — only idempotent echo of the same value is accepted).</li>
     *   <li>Otherwise, the new {@code validityPeriod.start} must be greater than or equal to
     *       {@code now()}.</li>
     *   <li>If provided, {@code validityPeriod.end} must be greater than or equal to
     *       {@code now()}.</li>
     *   <li>If provided, {@code suspensionPeriod.start} must be greater than or equal to
     *       {@code validityPeriod.start}.</li>
     *   <li>If provided, {@code suspensionPeriod.start} must be greater than or equal to
     *       {@code now()}.</li>
     *   <li>If {@code validityPeriod.end} is defined (request or persisted fallback), both
     *       {@code suspensionPeriod.start} and {@code suspensionPeriod.end} must be less than
     *       or equal to {@code validityPeriod.end}.</li>
     * </ul>
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the new status values
     * @return the refreshed account view, including computed {@code status} and
     *         {@code daysBeforeDeactivation}
     * @throws io.github.linagora.linid.im.corelib.exception.ApiException 404 if the account or its
     *         companion status row is not found; 400 if any business rule is violated
     */
    AccountView updateStatus(UserPrincipal userPrincipal, UUID accountId, AccountStatusRecord record);

    /**
     * Sets the {@code activationAt} timestamp of the account with the given identifier.
     *
     * <p>Business rules enforced (delegated to {@code AccountActivationValidator}):</p>
     * <ul>
     *   <li>The current {@code activationAt} must be {@code null}.</li>
     *   <li>The validity period must exist with a non-null start.</li>
     *   <li>The validity period's start must be less than or equal to {@code now()}.</li>
     *   <li>The provided {@code activationAt} must be greater than or equal to the validity start.</li>
     *   <li>The provided {@code activationAt} must be less than or equal to {@code now()}.</li>
     * </ul>
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the activation request carrying the new {@code activationAt}
     * @return the refreshed account view
     * @throws io.github.linagora.linid.im.corelib.exception.ApiException 404 if the account or its
     *         companion status row is not found; 400 if any business rule is violated
     */
    AccountView updateActivation(UserPrincipal userPrincipal, UUID accountId, AccountActivationRecord record);
}
