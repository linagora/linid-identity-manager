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

import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitReactivationRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRelationMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitSuspensionRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnit;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccountView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitStatus;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitAccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitRelationRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitStatusRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitViewRepository;
import io.github.linagora.linid.im.api.service.validation.OrganizationalUnitReactivationValidator;
import io.github.linagora.linid.im.api.service.validation.OrganizationalUnitSuspensionValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link OrganizationalUnitService}.
 *
 * <p>Provides access and management operations for organizational units and
 * their hierarchical relations.
 * <p>The root organizational unit is lazily initialized and cached after the
 * first successful lookup.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationalUnitServiceImpl implements OrganizationalUnitService {

    /**
     * Repository used to manage {@link OrganizationalUnit} persistence operations.
     */
    private final OrganizationalUnitRepository organizationalUnitRepository;

    /**
     * Repository used to manage {@link OrganizationalUnitView} persistence operations.
     */
    private final OrganizationalUnitViewRepository organizationalUnitViewRepository;

    /**
     * Repository used to manage {@link OrganizationalUnitAccountView} persistence operations.
     */
    private final OrganizationalUnitAccountViewRepository organizationalUnitAccountViewRepository;

    /**
     * Repository used to manage organizational unit relation persistence operations.
     */
    private final OrganizationalUnitRelationRepository organizationalUnitRelationRepository;

    /**
     * Mapper used to convert organizational unit entities and DTOs.
     */
    private final OrganizationalUnitMapper mapper;

    /**
     * Mapper used to convert organizational unit relation entities and DTOs.
     */
    private final OrganizationalUnitRelationMapper relationMapper;

    /**
     * Repository used to manage {@link OrganizationalUnitStatus} persistence operations.
     */
    private final OrganizationalUnitStatusRepository organizationalUnitStatusRepository;

    /**
     * Mapper applying organizational unit status mutations onto the persisted entity.
     */
    private final OrganizationalUnitStatusMapper organizationalUnitStatusMapper;

    /**
     * Shared mapper converting between API period records and persistence ranges, used to compute
     * the reconstructed periods carried by the status-mutation business logic.
     */
    private final CommonMapper commonMapper;

    /**
     * Validator enforcing the business rules of the organizational unit suspension flow.
     */
    private final OrganizationalUnitSuspensionValidator organizationalUnitSuspensionValidator;

    /**
     * Validator enforcing the business rules of the organizational unit reactivation flow.
     */
    private final OrganizationalUnitReactivationValidator organizationalUnitReactivationValidator;

    /**
     * Cached root organizational unit instance.
     * <p>Declared as {@code volatile} to ensure visibility across threads when
     * lazily initialized using double-checked locking.
     */
    private volatile OrganizationalUnit root;

    /**
     * Returns the root organizational unit.
     * <p>The root entity is lazily initialized on the first access and then cached
     * for subsequent calls.
     *
     * @return the root organizational unit
     * @throws ApiException if the root organizational unit cannot be found
     */
    public OrganizationalUnit getRoot() {
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    root = initRoot();
                }
            }
        }
        return root;
    }

    /**
     * Loads the root organizational unit from persistence storage.
     *
     * @return the root organizational unit
     * @throws ApiException if no root organizational unit exists
     */
    private OrganizationalUnit initRoot() {
        return organizationalUnitRepository.findByNameAndType("root", "root")
            .orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                I18nMessage.of("error.organizational.unit.root.not_found")
            ));
    }

    @Override
    public OrganizationalUnit create(final UserPrincipal userPrincipal,
                                     final OrganizationalUnitRecord organizationalUnit) {
        if ("root".equalsIgnoreCase(organizationalUnit.name()) || "root".equalsIgnoreCase(organizationalUnit.type())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.root")
            );
        }

        var parent = findById(userPrincipal, organizationalUnit.parent());

        if (organizationalUnitRepository.findByNameAndType(organizationalUnit.name(), organizationalUnit.type())
            .isPresent()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(
                    "error.organizational.unit.already_exists",
                    Map.of("name", organizationalUnit.name(), "type", organizationalUnit.type())
                )
            );
        }

        var entity = mapper.toEntity(organizationalUnit, userPrincipal);

        entity = organizationalUnitRepository.save(entity);

        var relation = relationMapper.toEntity(entity, parent, userPrincipal);

        organizationalUnitRelationRepository.save(relation);

        return entity;
    }

    @Override
    public void existsById(final UserPrincipal userPrincipal, final UUID id) {
        if (organizationalUnitRepository.existsById(id)) {
            return;
        }

        throw new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.organizational.unit.not_found", Map.of("id", id.toString()))
        );
    }

    @Override
    public OrganizationalUnit findById(final UserPrincipal userPrincipal, final UUID id) {
        return organizationalUnitRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    public OrganizationalUnitView findViewById(final UserPrincipal userPrincipal, final UUID id) {
        return organizationalUnitViewRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    public Page<OrganizationalUnitView> findAll(final UserPrincipal userPrincipal,
                                                final OrganizationalUnitViewQueryFilterDto filters,
                                                final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(OrganizationalUnitView.class, filters);

        return organizationalUnitViewRepository.findAll(specification, pageable);
    }

    @Override
    public Page<OrganizationalUnitAccountView> findAllAccounts(
        final UserPrincipal userPrincipal,
        final OrganizationalUnitAccountViewQueryFilterDto filters,
        final Pageable pageable) {
        var specification = new SpringQueryFilterSpecification<>(OrganizationalUnitAccountView.class, filters);

        return organizationalUnitAccountViewRepository.findAll(specification, pageable);
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        if (getRoot().getId().equals(id)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.root.delete")
            );
        }

        existsById(userPrincipal, id);

        organizationalUnitRepository.deleteById(id);
    }

    @Override
    public OrganizationalUnit update(final UserPrincipal userPrincipal,
                                     final UUID id,
                                     final OrganizationalUnitRecord organizationalUnit) {
        if (getRoot().getId().equals(id)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.root.update")
            );
        }

        if ("root".equalsIgnoreCase(organizationalUnit.name())
            || "root".equalsIgnoreCase(organizationalUnit.type())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.organizational.unit.root")
            );
        }

        var specifications = new SpringQueryFilterSpecification<>(OrganizationalUnit.class, Map.of(
            "id", List.of("not_" + id.toString()),
            "name", List.of(organizationalUnit.name()),
            "type", List.of(organizationalUnit.type())
        ));

        if (!organizationalUnitRepository.findAll(specifications).isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of(
                    "error.organizational.unit.already_exists",
                    Map.of("name", organizationalUnit.name(), "type", organizationalUnit.type())
                )
            );
        }

        var entity = findById(userPrincipal, id);

        if (entity.getName().equals(organizationalUnit.name())
            && entity.getType().equals(organizationalUnit.type())) {
            return entity;
        }

        entity.setName(organizationalUnit.name());
        entity.setType(organizationalUnit.type());

        return organizationalUnitRepository.save(entity);
    }

    @Override
    public OrganizationalUnitView suspend(final UserPrincipal userPrincipal,
                                          final UUID id,
                                          final OrganizationalUnitSuspensionRecord record) {
        ensureOrganizationalUnitExists(id);
        OrganizationalUnitStatus status = loadStatus(id);

        organizationalUnitSuspensionValidator.validate(status, record, id);

        organizationalUnitStatusMapper.applySuspension(status, record, userPrincipal.getId());
        organizationalUnitStatusRepository.saveAndFlush(status);

        return findViewById(userPrincipal, id);
    }

    @Override
    public OrganizationalUnitView reactivate(final UserPrincipal userPrincipal,
                                             final UUID id,
                                             final OrganizationalUnitReactivationRecord record) {
        ensureOrganizationalUnitExists(id);
        OrganizationalUnitStatus status = loadStatus(id);

        organizationalUnitReactivationValidator.validate(status, record, id);

        // A scheduled (not-yet-started) suspension is cancelled outright, since ending it at now would
        // produce an invalid range; an already-started one is simply ended now. Its reason fields are cleared.
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime suspensionStart = commonMapper.startOf(status.getSuspensionPeriod());
        if (suspensionStart != null && suspensionStart.isAfter(now)) {
            status.setSuspensionPeriod(null);
        } else {
            status.setSuspensionPeriod(commonMapper.toRange(new PeriodRecord(suspensionStart, now)));
        }

        status.setSuspensionReason(null);
        status.setSuspensionSubreason(null);
        status.setSuspensionComment(null);
        organizationalUnitStatusMapper.applyReactivation(status, record, userPrincipal.getId());
        organizationalUnitStatusRepository.saveAndFlush(status);

        return findViewById(userPrincipal, id);
    }

    /**
     * Ensures the organizational unit with the given identifier exists.
     *
     * @param id the organizational unit UUID
     * @throws ApiException with key {@code error.organizational.unit.not_found} (HTTP 404) when absent
     */
    private void ensureOrganizationalUnitExists(final UUID id) {
        if (!organizationalUnitRepository.existsById(id)) {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.not_found", Map.of("id", id.toString()))
            );
        }
    }

    /**
     * Loads the persisted {@link OrganizationalUnitStatus} of an organizational unit, or throws a 404
     * when absent.
     *
     * @param id the organizational unit UUID
     * @return the persisted status
     * @throws ApiException with key {@code error.organizational.unit.status.not_found} (HTTP 404)
     */
    private OrganizationalUnitStatus loadStatus(final UUID id) {
        return organizationalUnitStatusRepository.findByOrganizationalUnitId(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.status.not_found",
                    Map.of("id", id.toString()))
            ));
    }
}
