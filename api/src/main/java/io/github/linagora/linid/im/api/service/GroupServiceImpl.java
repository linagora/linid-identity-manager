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

import io.github.linagora.linid.im.api.model.group.GroupMapper;
import io.github.linagora.linid.im.api.model.group.GroupRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Group;
import io.github.linagora.linid.im.api.persistence.model.GroupView;
import io.github.linagora.linid.im.api.persistence.model.GroupViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.GroupRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link GroupService}.
 *
 * <p>Provides CRUD operations for groups, enforcing the uniqueness of the group code, the existence of the
 * referenced parent group, organizational unit and application, and the absence of cycles in the group
 * hierarchy.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImpl implements GroupService {

    /**
     * Repository used to manage {@link Group} persistence operations.
     */
    private final GroupRepository groupRepository;

    /**
     * Repository used to manage {@link GroupView} persistence operations.
     */
    private final GroupViewRepository groupViewRepository;

    /**
     * Service used to check the existence of the referenced organizational unit.
     */
    private final OrganizationalUnitService organizationalUnitService;

    /**
     * Service used to check the existence of the referenced application.
     */
    private final ApplicationService applicationService;

    /**
     * Mapper used to convert group entities and DTOs.
     */
    private final GroupMapper mapper;

    @Override
    public Group create(final UserPrincipal userPrincipal, final GroupRecord group) {
        if (groupRepository.existsByCode(group.code())) {
            throw codeAlreadyExists(group.code());
        }

        ensureReferencesExist(userPrincipal, group);
        ensureParentIsValid(null, group.parentId());

        var entity = mapper.toEntity(group, userPrincipal);

        return groupRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupView> findAll(final UserPrincipal userPrincipal,
                                   final GroupViewQueryFilterDto filters,
                                   final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(GroupView.class, filters);

        return groupViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Group findById(final UserPrincipal userPrincipal, final UUID id) {
        return groupRepository.findById(id).orElseThrow(() -> groupNotFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupView findViewById(final UserPrincipal userPrincipal, final UUID id) {
        return groupViewRepository.findById(id).orElseThrow(() -> groupNotFound(id));
    }

    @Override
    public Group update(final UserPrincipal userPrincipal, final UUID id, final GroupRecord group) {
        var entity = findById(userPrincipal, id);

        if (groupRepository.existsByCodeAndIdNot(group.code(), id)) {
            throw codeAlreadyExists(group.code());
        }

        ensureReferencesExist(userPrincipal, group);
        ensureParentIsValid(id, group.parentId());

        entity.setCode(group.code());
        entity.setLabel(group.label());
        entity.setParentId(group.parentId());
        entity.setDescription(group.description());
        entity.setEmail(group.email());
        entity.setOrganizationalUnitId(group.organizationalUnitId());
        entity.setApplicationId(group.applicationId());
        entity.setExtraParameters(group.extraParameters());
        entity.setUpdatedBy(userPrincipal.getId());

        return groupRepository.save(entity);
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        var entity = findById(userPrincipal, id);

        groupRepository.delete(entity);
    }

    /**
     * Ensures the organizational unit and the application referenced by the record exist, delegating the 404
     * {@link ApiException} to their own services. Absent references are skipped.
     *
     * @param userPrincipal the authenticated user
     * @param group         the request record carrying the optional references
     */
    private void ensureReferencesExist(final UserPrincipal userPrincipal, final GroupRecord group) {
        if (group.organizationalUnitId() != null) {
            organizationalUnitService.existsById(userPrincipal, group.organizationalUnitId());
        }

        if (group.applicationId() != null) {
            applicationService.findById(userPrincipal, group.applicationId());
        }
    }

    /**
     * Ensures the candidate parent group exists and that attaching the group to it does not create a cycle, by
     * walking up the parent chain from the candidate parent and checking the group itself is never reached. A
     * group being its own parent is the shortest such cycle. The walk stops on a group already visited, so a
     * cycle already stored in the hierarchy cannot make it loop forever.
     *
     * @param id       the identifier of the group being updated, {@code null} on creation
     * @param parentId the identifier of the candidate parent group, possibly {@code null}
     */
    private void ensureParentIsValid(final UUID id, final UUID parentId) {
        var visited = new HashSet<UUID>();
        var current = parentId;

        while (current != null && visited.add(current)) {
            if (current.equals(id)) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST.value(),
                    I18nMessage.of("error.group.parent.cycle",
                        Map.of("id", id.toString(), "parentId", parentId.toString()))
                );
            }

            var lookup = current;
            current = groupRepository.findById(lookup).orElseThrow(() -> groupNotFound(lookup)).getParentId();
        }
    }

    /**
     * Builds the 400 {@link ApiException} raised when a group code is already used.
     *
     * @param code the conflicting group code
     * @return the exception to throw
     */
    private ApiException codeAlreadyExists(final String code) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.group.code.already_exists", Map.of("code", code))
        );
    }

    /**
     * Builds the 404 {@link ApiException} for a missing group.
     *
     * @param id the group identifier
     * @return the exception to throw
     */
    private ApiException groupNotFound(final UUID id) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.group.not_found", Map.of("id", id.toString()))
        );
    }
}
