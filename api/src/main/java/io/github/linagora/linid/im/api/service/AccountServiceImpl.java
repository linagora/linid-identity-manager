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

import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link AccountService}.
 *
 * <p>Handles account CRUD operations including SHA-256 checksum generation
 * on creation and dynamic filtering via {@code spring-query-filter}.</p>
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
     * Service for computing SHA-256 checksums.
     */
    private final ChecksumService checksumService;

    @Override
    public Account create(final UserPrincipal userPrincipal, final AccountRecord account) {
        Account entity = new Account();
        entity.setExternalId(account.externalId());
        entity.setLastname(account.lastname());
        entity.setFirstname(account.firstname());
        entity.setEmail(account.email().toLowerCase());
        entity.setPayload(DEFAULT_PAYLOAD);
        entity.setChecksum(checksumService.compute(DEFAULT_PAYLOAD));
        entity.setCreatedBy(userPrincipal.getId());
        entity.setUpdatedBy(userPrincipal.getId());

        return accountRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Account> findAll(final UserPrincipal userPrincipal, final AccountQueryFilterDto filters,
                                 final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(Account.class, filters);
        return accountRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Account findById(final UserPrincipal userPrincipal, final UUID id) {
        return accountRepository.findById(id)
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
}
