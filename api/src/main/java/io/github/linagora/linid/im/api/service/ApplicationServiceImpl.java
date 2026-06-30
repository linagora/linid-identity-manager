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

import io.github.linagora.linid.im.api.model.application.ApplicationMapper;
import io.github.linagora.linid.im.api.model.application.ApplicationRecord;
import io.github.linagora.linid.im.api.model.application.ApplicationRolesRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link ApplicationService}.
 *
 * <p>Provides CRUD operations for applications, enforcing the uniqueness of the application code.
 * The {@code script}, its {@code scriptChecksum}, the {@code deployedAt} date and the {@code
 * configuration} are managed by a separate process and are never set from the API requests.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    /**
     * Repository used to manage {@link Application} persistence operations.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Repository used to manage {@link ApplicationView} persistence operations.
     */
    private final ApplicationViewRepository applicationViewRepository;

    /**
     * Mapper used to convert application entities and DTOs.
     */
    private final ApplicationMapper mapper;

    @Override
    public Application create(final UserPrincipal userPrincipal, final ApplicationRecord application) {
        if (applicationRepository.findByCode(application.code()).isPresent()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.application.code.already_exists", Map.of("code", application.code()))
            );
        }

        var entity = mapper.toEntity(application, userPrincipal);

        return applicationRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationView> findAll(final UserPrincipal userPrincipal,
                                         final ApplicationViewQueryFilterDto filters,
                                         final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(ApplicationView.class, filters);

        return applicationViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Application findById(final UserPrincipal userPrincipal, final UUID id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView findViewById(final UserPrincipal userPrincipal, final UUID id) {
        return applicationViewRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    public Application update(final UserPrincipal userPrincipal, final UUID id,
                              final ApplicationRecord application) {
        var entity = findById(userPrincipal, id);

        var specifications = new SpringQueryFilterSpecification<>(Application.class, Map.of(
            "id", List.of("not_" + id.toString()),
            "code", List.of(application.code())
        ));

        if (!applicationRepository.findAll(specifications).isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.application.code.already_exists", Map.of("code", application.code()))
            );
        }

        entity.setCode(application.code());
        entity.setName(application.name());
        entity.setDescription(application.description());
        entity.setType(application.type());
        entity.setClaimsTemplate(application.claimsTemplate());
        entity.setUpdatedBy(userPrincipal.getId());

        return applicationRepository.save(entity);
    }

    @Override
    public Application updateRoles(final UserPrincipal userPrincipal, final UUID id,
                                   final ApplicationRolesRecord record) {
        var entity = findById(userPrincipal, id);

        entity.setRoles(record.roles());
        entity.setUpdatedBy(userPrincipal.getId());

        return applicationRepository.save(entity);
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        if (!applicationRepository.existsById(id)) {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", id.toString()))
            );
        }

        applicationRepository.deleteById(id);
    }
}
