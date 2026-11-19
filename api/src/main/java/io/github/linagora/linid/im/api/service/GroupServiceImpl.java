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

import io.github.linagora.linid.im.api.model.group.GroupAccountRecord;
import io.github.linagora.linid.im.api.model.group.GroupManagerRecord;
import io.github.linagora.linid.im.api.model.group.GroupMapper;
import io.github.linagora.linid.im.api.model.group.GroupRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Group;
import io.github.linagora.linid.im.api.persistence.model.GroupAccount;
import io.github.linagora.linid.im.api.persistence.model.GroupAccountView;
import io.github.linagora.linid.im.api.persistence.model.GroupAccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.GroupManager;
import io.github.linagora.linid.im.api.persistence.model.GroupManagerView;
import io.github.linagora.linid.im.api.persistence.model.GroupManagerViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.GroupView;
import io.github.linagora.linid.im.api.persistence.model.GroupViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.GroupAccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupAccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupAncestorRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupManagerRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupManagerViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.CHECK_VIOLATION;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.FOREIGN_KEY_VIOLATION;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.UNIQUE_VIOLATION;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.hasSqlState;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.violatedConstraint;

/**
 * Default implementation of {@link GroupService}.
 *
 * <p>Provides CRUD operations for groups, enforcing the uniqueness of the group code, the existence of the
 * referenced parent group, organizational unit and application, and the absence of cycles in the group
 * hierarchy. Also manages the accounts attached to a group and the managers of a group.</p>
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
     * Repository used to read the ancestors of a group from the ancestors view over the group closure table.
     */
    private final GroupAncestorRepository groupAncestorRepository;

    /**
     * Repository used to manage {@link GroupAccount} relationship persistence operations.
     */
    private final GroupAccountRepository groupAccountRepository;

    /**
     * Repository used to manage {@link GroupAccountView} persistence operations.
     */
    private final GroupAccountViewRepository groupAccountViewRepository;

    /**
     * Repository used to manage {@link GroupManager} persistence operations.
     */
    private final GroupManagerRepository groupManagerRepository;

    /**
     * Repository used to read {@link GroupManagerView} projections.
     */
    private final GroupManagerViewRepository groupManagerViewRepository;

    /**
     * Service used to check the existence of the accounts to attach or to add as managers.
     */
    private final AccountService accountService;

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
    @Transactional(readOnly = true)
    public void existsById(final UserPrincipal userPrincipal, final UUID id) {
        if (groupRepository.existsById(id)) {
            return;
        }

        throw groupNotFound(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupAccountView> findAllAccounts(final UserPrincipal userPrincipal,
                                                  final GroupAccountViewQueryFilterDto filters,
                                                  final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(GroupAccountView.class, filters);

        return groupAccountViewRepository.findAll(specification, pageable);
    }

    @Override
    public GroupAccount attachAccount(final UserPrincipal userPrincipal,
                                      final UUID groupId,
                                      final GroupAccountRecord record) {
        existsById(userPrincipal, groupId);
        accountService.existsById(userPrincipal, record.accountId());

        if (groupAccountRepository.existsByGroupIdAndAccountId(groupId, record.accountId())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.group.account.already_attached",
                    Map.of("id", record.accountId().toString()))
            );
        }

        var entity = GroupAccount.builder()
            .groupId(groupId)
            .accountId(record.accountId())
            .extraParameters(Objects.requireNonNullElseGet(record.extraParameters(), HashMap::new))
            .createdBy(userPrincipal.getId())
            .updatedBy(userPrincipal.getId())
            .build();

        return groupAccountRepository.save(entity);
    }

    @Override
    public void detachAccount(final UserPrincipal userPrincipal, final UUID groupId, final UUID accountId) {
        existsById(userPrincipal, groupId);

        var relation = groupAccountRepository.findByGroupIdAndAccountId(groupId, accountId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.group.account.not_attached", Map.of("id", accountId.toString()))
            ));

        groupAccountRepository.delete(relation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupManagerView> findAllManagers(final UserPrincipal userPrincipal,
                                                  final GroupManagerViewQueryFilterDto filters,
                                                  final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(GroupManagerView.class, filters);

        return groupManagerViewRepository.findAll(specification, pageable);
    }

    @Override
    public GroupManager addManager(final UserPrincipal userPrincipal,
                                   final UUID groupId,
                                   final GroupManagerRecord record) {
        existsById(userPrincipal, groupId);
        accountService.existsById(userPrincipal, record.accountId());

        if (groupManagerRepository.existsByGroupIdAndAccountId(groupId, record.accountId())) {
            throw managerAlreadyExists(record.accountId());
        }

        // Flush so that a rejection by the database surfaces here rather than at commit time: the same manager
        // added, or the group or the account deleted, by a concurrent request after the checks above.
        try {
            var manager = saveManager(userPrincipal, groupId, record.accountId());
            groupManagerRepository.flush();
            return manager;
        } catch (DataIntegrityViolationException e) {
            if (hasSqlState(e, UNIQUE_VIOLATION)) {
                throw managerAlreadyExists(record.accountId());
            }

            if (hasSqlState(e, FOREIGN_KEY_VIOLATION)) {
                throw managerReferenceNotFound(e, groupId, record.accountId());
            }

            throw e;
        }
    }

    @Override
    public void removeManager(final UserPrincipal userPrincipal, final UUID groupId, final UUID accountId) {
        existsById(userPrincipal, groupId);

        var manager = groupManagerRepository.findByGroupIdAndAccountId(groupId, accountId)
            .orElseThrow(() -> managerNotFound(accountId));

        // Flush so that a removal by a concurrent request after the lookup above surfaces here rather than at commit
        // time: the delete then matches no row, which Hibernate reports as an optimistic lock failure.
        try {
            groupManagerRepository.delete(manager);
            groupManagerRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw managerNotFound(accountId);
        }
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
        entity.setName(group.name());
        entity.setParentId(group.parentId());
        entity.setDescription(group.description());
        entity.setEmail(group.email());
        entity.setOrganizationalUnitId(group.organizationalUnitId());
        entity.setApplicationId(group.applicationId());

        if (group.extraParameters() != null) {
            entity.setExtraParameters(group.extraParameters());
        }

        entity.setUpdatedBy(userPrincipal.getId());

        try {
            // Flush so that a rejection by the database surfaces here rather than at commit time: a cycle, or a code
            // taken or a reference deleted by a concurrent request after the checks above.
            return groupRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // On an update of groups, each SQLState has a single cause: a check violation is a cycle (the
            // ck_groups_parent_id_not_self constraint or the group_closures_refresh() trigger), a unique violation is
            // the code, and a foreign key violation is a reference deleted by a concurrent request (the parent group,
            // the organizational unit or the application).
            if (hasSqlState(e, CHECK_VIOLATION)) {
                throw parentCycle(id, group.parentId());
            }

            if (hasSqlState(e, UNIQUE_VIOLATION)) {
                throw codeAlreadyExists(group.code());
            }

            if (hasSqlState(e, FOREIGN_KEY_VIOLATION)) {
                throw referenceNotFound(e, group);
            }

            throw e;
        }
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        var entity = findById(userPrincipal, id);

        groupRepository.delete(entity);
    }

    /**
     * Saves the given account as a manager of the given group, the authenticated user being the author.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param groupId       the identifier of the managed group
     * @param accountId     the identifier of the manager account
     * @return the saved {@link GroupManager}
     */
    private GroupManager saveManager(final UserPrincipal userPrincipal, final UUID groupId, final UUID accountId) {
        var entity = GroupManager.builder()
            .groupId(groupId)
            .accountId(accountId)
            .createdBy(userPrincipal.getId())
            .updatedBy(userPrincipal.getId())
            .build();

        return groupManagerRepository.save(entity);
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
     * Ensures the candidate parent group exists and that attaching the group to it does not create a cycle: the
     * group must be neither the parent itself nor one of the parent's ancestors, read from the ancestors view over
     * the group closure table. The database rejects cycles as well, including one closed by a concurrent parent
     * change committed after this check: {@link #update} flushes and turns that rejection into the same 400 error.
     *
     * @param id       the identifier of the group being updated, {@code null} on creation
     * @param parentId the identifier of the candidate parent group, possibly {@code null}
     */
    private void ensureParentIsValid(final UUID id, final UUID parentId) {
        if (parentId == null) {
            return;
        }

        if (!groupRepository.existsById(parentId)) {
            throw groupNotFound(parentId);
        }

        if (id == null) {
            return;
        }

        if (parentId.equals(id)) {
            throw parentCycle(id, parentId);
        }

        if (groupAncestorRepository.existsByGroupIdAndAncestorId(parentId, id)) {
            throw parentCycle(id, parentId);
        }
    }

    /**
     * Builds the 404 {@link ApiException} of the reference whose foreign key the database rejected. The foreign keys
     * of {@code groups} are unnamed: PostgreSQL names them {@code groups_<column>_fkey}.
     *
     * @param exception the foreign key violation raised by the flush
     * @param group     the request record carrying the references
     * @return the 404 exception of the missing reference, or {@code exception} when its constraint is not known
     */
    private RuntimeException referenceNotFound(final DataIntegrityViolationException exception,
                                               final GroupRecord group) {
        return switch (violatedConstraint(exception)) {
            case "groups_parent_id_fkey" -> groupNotFound(group.parentId());
            case "groups_oun_id_fkey" -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.not_found",
                    Map.of("id", group.organizationalUnitId().toString()))
            );
            case "groups_app_id_fkey" -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", group.applicationId().toString()))
            );
            case null, default -> exception;
        };
    }

    /**
     * Builds the 400 {@link ApiException} raised when a parent would create a cycle in the group hierarchy.
     *
     * @param id       the identifier of the group being updated
     * @param parentId the identifier of the rejected parent group
     * @return the exception to throw
     */
    private ApiException parentCycle(final UUID id, final UUID parentId) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.group.parent.cycle", Map.of("id", id.toString(), "parentId", parentId.toString()))
        );
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
     * Builds the 400 {@link ApiException} raised when an account is already a manager of a group.
     *
     * @param accountId the identifier of the account already manager
     * @return the exception to throw
     */
    private ApiException managerAlreadyExists(final UUID accountId) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.group.manager.already_exists", Map.of("id", accountId.toString()))
        );
    }

    /**
     * Builds the 404 {@link ApiException} raised when an account is not a manager of a group.
     *
     * @param accountId the identifier of the account that is not a manager
     * @return the exception to throw
     */
    private ApiException managerNotFound(final UUID accountId) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.group.manager.not_found", Map.of("id", accountId.toString()))
        );
    }

    /**
     * Builds the 404 {@link ApiException} of the group or the account whose foreign key the database rejected. The
     * foreign keys of {@code group_managers} are unnamed: PostgreSQL names them {@code group_managers_<column>_fkey}.
     *
     * @param exception the foreign key violation raised by the flush
     * @param groupId   the group identifier
     * @param accountId the account identifier
     * @return the 404 exception of the missing reference, or {@code exception} when its constraint is not known
     */
    private RuntimeException managerReferenceNotFound(final DataIntegrityViolationException exception,
                                                      final UUID groupId,
                                                      final UUID accountId) {
        return switch (violatedConstraint(exception)) {
            case "group_managers_grp_id_fkey" -> groupNotFound(groupId);
            case "group_managers_act_id_fkey" -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", accountId.toString()))
            );
            case null, default -> exception;
        };
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
