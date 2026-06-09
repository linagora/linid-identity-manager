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
import io.github.linagora.linid.im.api.model.account.AccountDeactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountReactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountSuspensionRecord;
import io.github.linagora.linid.im.api.model.account.AccountValidityRecord;
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
     */
    AccountView findById(UserPrincipal userPrincipal, UUID id);

    /**
     * Deletes an account by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
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
     * Suspends the account with the given identifier, either immediately or as a scheduled suspension,
     * after enforcing the business rules delegated to {@code AccountSuspensionValidator}.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the suspension request (suspension period and reason fields)
     * @return the refreshed account view
     */
    AccountView suspend(UserPrincipal userPrincipal, UUID accountId, AccountSuspensionRecord record);

    /**
     * Deactivates the account with the given identifier (sets its validity period end), either
     * immediately or as a scheduled deactivation, after enforcing the business rules delegated to
     * {@code AccountDeactivationValidator}.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the deactivation request (deactivation timestamp and reason fields)
     * @return the refreshed account view
     */
    AccountView deactivate(UserPrincipal userPrincipal, UUID accountId, AccountDeactivationRecord record);

    /**
     * Reactivates the account with the given identifier (lifts its suspension), after enforcing the
     * business rules delegated to {@code AccountReactivationValidator}.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the reactivation request (mandatory justification comment)
     * @return the refreshed account view
     */
    AccountView reactivate(UserPrincipal userPrincipal, UUID accountId, AccountReactivationRecord record);

    /**
     * Schedules the validity period start of the account with the given identifier, after enforcing
     * the business rules delegated to {@code AccountValidityValidator}. The existing validity period
     * end is preserved. This is distinct from {@link #updateActivation} which records the
     * {@code activationAt} timestamp.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param record        the validity request (validity period start)
     * @return the refreshed account view
     */
    AccountView updateValidity(UserPrincipal userPrincipal, UUID accountId, AccountValidityRecord record);

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
     */
    AccountView updateActivation(UserPrincipal userPrincipal, UUID accountId, AccountActivationRecord record);
}
