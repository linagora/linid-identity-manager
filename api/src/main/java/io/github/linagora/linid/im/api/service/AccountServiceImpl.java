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
import io.github.linagora.linid.im.api.model.account.AccountMapper;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountStatusMapper;
import io.github.linagora.linid.im.api.model.account.AccountStatusRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountStatusRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountViewRepository;
import io.github.linagora.linid.im.api.service.validation.AccountActivationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountCreationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountStatusValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AccountService}.
 *
 * <p>Handles account CRUD operations including SHA-256 checksum generation
 * on creation and dynamic filtering via {@code spring-query-filter}.</p>
 *
 * <p><b>Note for status-mutation endpoints:</b> {@link AccountView} is mapped to the
 * {@code accounts_view} SQL view, which is computed by joining {@code accounts} and
 * {@code account_status}. Hibernate's first-level cache cannot detect that a write to
 * {@code account_status} invalidates a previously loaded {@link AccountView} instance.
 * To return a fresh view to the caller, methods that mutate {@link AccountStatus} must:
 * <ul>
 *   <li>use {@link AccountRepository#existsById} (not {@link #findById}) for the
 *       existence precondition, so the view is not loaded into the persistence context;</li>
 *   <li>load the persisted {@link AccountStatus} via
 *       {@link AccountStatusRepository#findByAccountId} and {@code orElseThrow} a 404 — every
 *       account is created with a companion status row in {@link #create}, so a missing row
 *       indicates a data integrity issue, not a normal flow;</li>
 *   <li>call {@code saveAndFlush} on {@link AccountStatusRepository} to push the changes
 *       to the database before reading the view;</li>
 *   <li>read the view only once, at the end of the operation, via {@link #findById}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    /**
     * Default JSON payload assigned to newly created accounts.
     */
    private static final String DEFAULT_PAYLOAD = "{}";

    /**
     * Repository for account persistence operations.
     */
    private final AccountRepository accountRepository;

    /**
     * Repository for read-only account view operations, supporting dynamic filtering.
     */
    private final AccountViewRepository accountViewRepository;

    /**
     * Service for computing SHA-256 checksums.
     */
    private final ChecksumService checksumService;

    /**
     * Repository for account status persistence operations.
     */
    private final AccountStatusRepository accountStatusRepository;

    /**
     * Mapper for converting account records into {@link Account} entities.
     */
    private final AccountMapper accountMapper;

    /**
     * Mapper applying status updates on account status entities.
     */
    private final AccountStatusMapper accountStatusMapper;

    /**
     * Validator enforcing the business rules of the account activation flow.
     */
    private final AccountActivationValidator accountActivationValidator;

    /**
     * Validator enforcing the business rules applied when updating account status fields.
     */
    private final AccountStatusValidator accountStatusValidator;

    /**
     * Validator enforcing the business rules of the account creation flow.
     */
    private final AccountCreationValidator accountCreationValidator;

    @Override
    public Account create(final UserPrincipal userPrincipal, final AccountRecord account) {

        accountCreationValidator.validate(account);

        Account entity = accountMapper.toAccount(account, userPrincipal);
        entity.setPayload(DEFAULT_PAYLOAD);
        entity.setChecksum(checksumService.compute(DEFAULT_PAYLOAD));

        Account createdAccount = accountRepository.save(entity);

        AccountStatus statusEntity = accountStatusMapper.toAccountStatus(account, userPrincipal, createdAccount);
        accountStatusRepository.save(statusEntity);

        return createdAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountView> findAll(final UserPrincipal userPrincipal,
                                     final AccountViewQueryFilterDto filters,
                                     final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(AccountView.class, filters);

        return accountViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView findById(final UserPrincipal userPrincipal, final UUID id) {
        return accountViewRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        findById(userPrincipal, id);
        accountRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> getAccountByEmail(final String email) {
        if (StringUtils.isBlank(email)) {
            return Optional.empty();
        }

        return accountRepository.findAccountByEmail(email.toLowerCase());
    }

    @Override
    public AccountView updateStatus(final UserPrincipal userPrincipal,
                                    final UUID accountId,
                                    final AccountStatusRecord record) {
        ensureAccountExists(accountId);

        AccountStatus status = accountStatusRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.status.not_found",
                    Map.of("id", accountId.toString()))
            ));

        accountStatusValidator.validate(status, record, accountId);

        AccountStatus updatedStatus = accountStatusMapper.toAccountStatus(status, record, userPrincipal);
        accountStatusRepository.saveAndFlush(updatedStatus);

        return findById(userPrincipal, accountId);
    }

    @Override
    public AccountView updateActivation(final UserPrincipal userPrincipal,
                                        final UUID accountId,
                                        final AccountActivationRecord record) {
        ensureAccountExists(accountId);

        AccountStatus status = accountStatusRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.status.not_found",
                    Map.of("id", accountId.toString()))
            ));

        accountActivationValidator.validate(status, record, accountId);

        status.setActivationAt(record.activationAt());
        status.setUpdatedBy(userPrincipal.getId());
        accountStatusRepository.saveAndFlush(status);

        return findById(userPrincipal, accountId);
    }

    /**
     * Ensures the account with the given identifier exists, without loading the read-only view.
     *
     * <p>Used as a precondition by the status-mutation endpoints to keep the {@code accounts_view}
     * row out of the persistence context until the new status has been flushed.</p>
     *
     * @param accountId the account UUID
     * @throws ApiException with key {@code error.account.not_found} (HTTP 404) when no account matches
     */
    private void ensureAccountExists(final UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", accountId.toString()))
            );
        }
    }
}
