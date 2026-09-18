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

import io.github.linagora.linid.im.api.model.role.RoleMapper;
import io.github.linagora.linid.im.api.model.role.RoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Role;
import io.github.linagora.linid.im.api.persistence.model.RoleDistinctView;
import io.github.linagora.linid.im.api.persistence.model.RoleView;
import io.github.linagora.linid.im.api.persistence.model.RoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitAccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.RoleRepository;
import io.github.linagora.linid.im.api.persistence.repository.RoleDistinctViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.executor.SpringQueryExecutor;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link RoleService}.
 *
 * <p>Handles functional role CRUD operations and dynamic filtering via
 * {@code spring-query-filter}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    /**
     * Repository for role persistence operations.
     */
    private final RoleRepository roleRepository;

    /**
     * Repository for read-only distinct role view operations.
     */
    private final RoleDistinctViewRepository roleDistinctViewRepository;

    private final OrganizationalUnitAccountRepository organizationalUnitAccountRepository;

    /**
     * Executor building distinct projections from filtered view queries.
     */
    private final SpringQueryExecutor executor;

    /**
     * Mapper for converting role records into {@link Role} entities.
     */
    private final RoleMapper roleMapper;

    @Override
    public Role create(final UserPrincipal userPrincipal, final RoleRecord role) {
        if (roleRepository.existsByCode(role.code())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(
                    "error.role.code.already_exists",
                    Map.of("code", role.code())
                ));
        }

        Role entity = roleMapper.toRole(role, userPrincipal);
        return roleRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleDistinctView> findAll(final UserPrincipal userPrincipal,
                                          final RoleViewQueryFilterDto filters,
                                          final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(RoleView.class, filters);

        return executor.findDistinctPageEntities(
                RoleView.class,
                RoleDistinctView.class,
                specification,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDistinctView findById(final UserPrincipal userPrincipal, final UUID id) {
        return roleDistinctViewRepository.findFirstById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND.value(),
                        I18nMessage.of(
                                "error.role.not_found",
                                Map.of("id", id.toString())
                        )
                ));
    }

    @Override
    public RoleDistinctView update(final UserPrincipal userPrincipal, final UUID roleId, final RoleRecord record) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND.value(),
                        I18nMessage.of(
                                "error.role.not_found",
                                Map.of("id", roleId.toString())
                        )
                ));

        if (roleRepository.existsByCodeAndIdNot(record.code(), role.getId())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(
                    "error.role.code.already_exists",
                    Map.of("code", record.code())
                ));
        }

        role.setCode(record.code());
        role.setName(record.name());
        role.setDescription(record.description());
        role.setUpdatedBy(userPrincipal.getId());

        if (record.extraParameters() != null) {
            role.setExtraParameters(record.extraParameters());
        }

        roleRepository.saveAndFlush(role);

        return findById(userPrincipal, roleId);
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        findById(userPrincipal, id);

        if (organizationalUnitAccountRepository.existsByRoleId(id)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST.value(),
                    I18nMessage.of("error.role.in_use", Map.of("id", id.toString()))
            );
        }

        roleRepository.deleteById(id);
    }
}
