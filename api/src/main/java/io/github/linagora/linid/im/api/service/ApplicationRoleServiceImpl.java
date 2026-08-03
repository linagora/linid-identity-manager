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

import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleMapper;
import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRole;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRoleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRoleViewRepository;
import io.github.linagora.linid.im.api.service.validation.SystemApplicationValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ApplicationRoleService}.
 *
 * <p>Enforces the existence of the owning application and the uniqueness of the role {@code name} within
 * that application.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationRoleServiceImpl implements ApplicationRoleService {

    /**
     * Repository used to manage {@link ApplicationRole} persistence operations.
     */
    private final ApplicationRoleRepository applicationRoleRepository;

    /**
     * Repository used to manage {@link ApplicationRoleView} persistence operations.
     */
    private final ApplicationRoleViewRepository applicationRoleViewRepository;

    /**
     * Repository used to check the existence of the owning application.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Mapper used to convert application role entities and DTOs.
     */
    private final ApplicationRoleMapper mapper;

    /**
     * Validator protecting the roles of the system-reserved application from any mutation.
     */
    private final SystemApplicationValidator systemApplicationValidator;

    @Override
    public ApplicationRole create(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final ApplicationRoleRecord role) {
        ensureApplicationExists(applicationId);
        systemApplicationValidator.ensureRolesAreMutable(applicationId);

        if (applicationRoleRepository.existsByApplicationIdAndName(applicationId, role.name())) {
            throw nameAlreadyExists(role.name());
        }

        var entity = mapper.toEntity(role, userPrincipal);
        entity.setApplicationId(applicationId);

        return applicationRoleRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationRoleView> findAll(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final ApplicationRoleViewQueryFilterDto filters,
        final Pageable pageable) {
        ensureApplicationExists(applicationId);

        Specification<ApplicationRoleView> specification =
            new SpringQueryFilterSpecification<>(ApplicationRoleView.class, filters)
                .and(new SpringQueryFilterSpecification<>(ApplicationRoleView.class, Map.of(
                    "applicationId", List.of(applicationId.toString())
                )));

        return applicationRoleViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationRole findById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        ensureApplicationExists(applicationId);

        return applicationRoleRepository.findByIdAndApplicationId(id, applicationId)
            .orElseThrow(() -> roleNotFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationRoleView findViewById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        ensureApplicationExists(applicationId);

        return applicationRoleViewRepository.findByIdAndApplicationId(id, applicationId)
            .orElseThrow(() -> roleNotFound(id));
    }

    @Override
    public ApplicationRole update(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id,
        final ApplicationRoleRecord role) {
        var entity = findById(userPrincipal, applicationId, id);

        systemApplicationValidator.ensureRolesAreMutable(applicationId);

        if (!role.name().equals(entity.getName())
            && applicationRoleRepository.existsByApplicationIdAndNameAndIdNot(applicationId, role.name(), id)) {
            throw nameAlreadyExists(role.name());
        }

        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setUpdatedBy(userPrincipal.getId());

        return applicationRoleRepository.save(entity);
    }

    @Override
    public void deleteById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        var entity = findById(userPrincipal, applicationId, id);

        systemApplicationValidator.ensureRolesAreMutable(applicationId);

        applicationRoleRepository.delete(entity);
    }

    /**
     * Ensures the owning application exists, throwing a 404 {@link ApiException} otherwise.
     *
     * @param applicationId the identifier of the owning application
     */
    private void ensureApplicationExists(final UUID applicationId) {
        if (applicationRepository.existsById(applicationId)) {
            return;
        }

        throw new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.application.not_found", Map.of("id", applicationId.toString()))
        );
    }

    /**
     * Builds the 400 {@link ApiException} raised when a role name is already used in the application.
     *
     * @param name the conflicting role name
     * @return the exception to throw
     */
    private ApiException nameAlreadyExists(final String name) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.application_role.name.already_exists", Map.of("name", name))
        );
    }

    /**
     * Builds the 404 {@link ApiException} for a missing role.
     *
     * @param id the role identifier
     * @return the exception to throw
     */
    private ApiException roleNotFound(final UUID id) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.application_role.not_found", Map.of("id", id.toString()))
        );
    }
}
