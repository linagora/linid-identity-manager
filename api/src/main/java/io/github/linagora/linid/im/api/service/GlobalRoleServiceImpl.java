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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.GlobalRole;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleEnum;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.GlobalRoleRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link GlobalRoleService}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GlobalRoleServiceImpl implements GlobalRoleService {

    /**
     * Repository for global role persistence.
     */
    private final GlobalRoleRepository globalRoleRepository;

    /**
     * Repository used to check the existence of the account receiving the role.
     */
    private final AccountRepository accountRepository;

    @Override
    public GlobalRole create(final UserPrincipal userPrincipal, final UUID accountId, final String role) {
        var globalRole = toGlobalRole(role);

        if (!accountRepository.existsById(accountId)) {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", accountId.toString()))
            );
        }

        if (globalRoleRepository.existsByAccountIdAndRole(accountId, globalRole)) {
            throw alreadyAssigned(accountId, globalRole);
        }

        var entity = GlobalRole.builder()
            .accountId(accountId)
            .role(globalRole)
            .createdBy(userPrincipal.getId())
            .updatedBy(userPrincipal.getId())
            .build();

        // A concurrent request can assign the same role after the check: flush so the unique constraint
        // violation surfaces here rather than at commit time.
        try {
            return globalRoleRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw alreadyAssigned(accountId, globalRole);
        }
    }

    /**
     * Builds the error returned when the account already holds the role.
     *
     * @param accountId  the account UUID
     * @param globalRole the global role
     * @return an {@link ApiException} with HTTP 400
     */
    private ApiException alreadyAssigned(final UUID accountId, final GlobalRoleEnum globalRole) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.global_role.already_assigned",
                Map.of("id", accountId.toString(), "role", globalRole.name()))
        );
    }

    /**
     * Converts the given name into one of the stored global roles.
     *
     * @param role the global role name, case-sensitive
     * @return the matching {@link GlobalRoleEnum}
     * @throws ApiException with HTTP 400 when the name matches no stored global role ({@code SUPER_ADMIN} included)
     */
    private GlobalRoleEnum toGlobalRole(final String role) {
        return Arrays.stream(GlobalRoleEnum.values())
            .filter(value -> value.name().equals(role))
            .findFirst()
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.global_role.unknown", Map.of("role", role))
            ));
    }
}
