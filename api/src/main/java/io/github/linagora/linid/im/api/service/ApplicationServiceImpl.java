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
import io.github.linagora.linid.im.api.model.application.manager.ApplicationManagerRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.interceptor.RlsInterceptor;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationManager;
import io.github.linagora.linid.im.api.persistence.model.ApplicationManagerView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationManagerViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.ApplicationView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationManagerRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationManagerViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationViewRepository;
import io.github.linagora.linid.im.api.service.validation.SystemApplicationValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.FOREIGN_KEY_VIOLATION;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.UNIQUE_VIOLATION;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.hasSqlState;
import static io.github.linagora.linid.im.api.service.DataIntegrityViolations.violatedConstraint;

/**
 * Default implementation of {@link ApplicationService}.
 *
 * <p>Provides CRUD operations for applications, enforcing the uniqueness of the application code.
 * The {@code claimsTemplateChecksum} is computed from the {@code claimsTemplate}. The {@code script}, its
 * {@code scriptChecksum}, the {@code deployedAt} date and the {@code configuration} are managed by a separate
 * process and are never set from the API requests.</p>
 *
 * <p>Also manages the managers of an application, enforcing the existence of the manager account and the
 * uniqueness of an account among the managers of an application.</p>
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

    /**
     * Repository used to load the active rules of an application.
     */
    private final ApplicationRuleRepository applicationRuleRepository;

    /**
     * Service generating the OPA policy script from the active rules.
     */
    private final OpaService opaService;

    /**
     * Service computing the claims template and script checksums.
     */
    private final ChecksumService checksumService;

    /**
     * Validator protecting the system-reserved application from any mutation.
     */
    private final SystemApplicationValidator systemApplicationValidator;

    /**
     * Repository used to manage {@link ApplicationManager} persistence operations.
     */
    private final ApplicationManagerRepository applicationManagerRepository;

    /**
     * Repository used to read {@link ApplicationManagerView} projections.
     */
    private final ApplicationManagerViewRepository applicationManagerViewRepository;

    /**
     * Service used to check the existence of the account to add as a manager.
     */
    private final AccountService accountService;

    /**
     * Interceptor setting the RLS context of the calling user for the current transaction.
     */
    private final RlsInterceptor rlsInterceptor;

    @Override
    public Application create(final UserPrincipal userPrincipal, final ApplicationRecord application) {
        rlsInterceptor.securize(userPrincipal);

        if (applicationRepository.findByCode(application.code()).isPresent()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.application.code.already_exists", Map.of("code", application.code()))
            );
        }

        var entity = mapper.toEntity(application, userPrincipal);
        entity.setClaimsTemplateChecksum(checksumService.compute(application.claimsTemplate()));

        return applicationRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationView> findAll(final UserPrincipal userPrincipal,
                                         final ApplicationViewQueryFilterDto filters,
                                         final Pageable pageable) {
        rlsInterceptor.securize(userPrincipal);

        var specification = new SpringQueryFilterSpecification<>(ApplicationView.class, filters);

        return applicationViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Application findById(final UserPrincipal userPrincipal, final UUID id) {
        rlsInterceptor.securize(userPrincipal);

        return applicationRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public void existsById(final UserPrincipal userPrincipal, final UUID id) {
        rlsInterceptor.securize(userPrincipal);

        if (applicationRepository.existsById(id)) {
            return;
        }

        throw new ApiException(HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.application.not_found", Map.of("id", id.toString())));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView findViewById(final UserPrincipal userPrincipal, final UUID id) {
        rlsInterceptor.securize(userPrincipal);

        return applicationViewRepository.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", id.toString()))
            ));
    }

    @Override
    public Application update(final UserPrincipal userPrincipal, final UUID id,
                              final ApplicationRecord application) {
        rlsInterceptor.securize(userPrincipal);

        var entity = findById(userPrincipal, id);

        systemApplicationValidator.ensureApplicationIsMutable(entity);

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

        var claimsTemplateChecksum = checksumService.compute(application.claimsTemplate());

        if (!claimsTemplateChecksum.equals(entity.getClaimsTemplateChecksum())) {
            entity.setClaimsTemplateChecksum(claimsTemplateChecksum);
        }

        entity.setCode(application.code());
        entity.setName(application.name());
        entity.setDomain(application.domain());
        entity.setDescription(application.description());
        entity.setType(application.type());
        entity.setClaimsTemplate(application.claimsTemplate());

        if (application.extraParameters() != null) {
            entity.setExtraParameters(application.extraParameters());
        }

        entity.setUpdatedBy(userPrincipal.getId());

        return applicationRepository.save(entity);
    }

    @Override
    public void deleteById(final UserPrincipal userPrincipal, final UUID id) {
        rlsInterceptor.securize(userPrincipal);

        var entity = findById(userPrincipal, id);

        systemApplicationValidator.ensureApplicationIsMutable(entity);

        applicationRepository.delete(entity);
    }

    @Override
    public void regeneratePolicy(final UserPrincipal userPrincipal, final UUID applicationId) {
        rlsInterceptor.securize(userPrincipal);

        var application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", applicationId.toString()))
            ));

        var activeRules = applicationRuleRepository
            .findByApplicationIdAndDisabledFalseOrderByPriorityAsc(applicationId);

        var script = opaService.generate(application, activeRules);
        var checksum = checksumService.compute(script);

        if (checksum.equals(application.getScriptChecksum())) {
            // The generated script is unchanged: skip the update to avoid resetting the deployment status.
            return;
        }

        // TODO: replace once updatePolicy (raw script string) is reworked — workaround for stale @Version +
        //  @Generated updateDate after a prior operation in the same session
        applicationRepository.updatePolicy(applicationId, script, checksum);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationManagerView> findAllManagers(
        final UserPrincipal userPrincipal,
        final ApplicationManagerViewQueryFilterDto filters,
        final Pageable pageable) {
        rlsInterceptor.securize(userPrincipal);

        var specification = new SpringQueryFilterSpecification<>(ApplicationManagerView.class, filters);

        return applicationManagerViewRepository.findAll(specification, pageable);
    }

    @Override
    public ApplicationManager addManager(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final ApplicationManagerRecord record) {
        rlsInterceptor.securize(userPrincipal);

        existsById(userPrincipal, applicationId);
        accountService.existsById(userPrincipal, record.accountId());

        if (applicationManagerRepository.existsByApplicationIdAndAccountId(applicationId, record.accountId())) {
            throw managerAlreadyExists(record.accountId());
        }

        var entity = ApplicationManager.builder()
            .applicationId(applicationId)
            .accountId(record.accountId())
            .createdBy(userPrincipal.getId())
            .updatedBy(userPrincipal.getId())
            .build();

        // Flush so that a rejection by the database surfaces here rather than at commit time: the same manager
        // added, or the application or the account deleted, by a concurrent request after the checks above.
        try {
            return applicationManagerRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (hasSqlState(e, UNIQUE_VIOLATION)) {
                throw managerAlreadyExists(record.accountId());
            }

            if (hasSqlState(e, FOREIGN_KEY_VIOLATION)) {
                throw managerReferenceNotFound(e, applicationId, record.accountId());
            }

            throw e;
        }
    }

    @Override
    public void removeManager(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID accountId) {
        rlsInterceptor.securize(userPrincipal);

        existsById(userPrincipal, applicationId);

        var manager = applicationManagerRepository.findByApplicationIdAndAccountId(applicationId, accountId)
            .orElseThrow(() -> managerNotFound(accountId));

        // Flush so that a removal by a concurrent request after the lookup above surfaces here rather than at commit
        // time: the delete then matches no row, which Hibernate reports as an optimistic lock failure.
        try {
            applicationManagerRepository.delete(manager);
            applicationManagerRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw managerNotFound(accountId);
        }
    }

    /**
     * Builds the error returned when the account is not a manager of the application.
     *
     * @param accountId the account identifier
     * @return an {@link ApiException} with HTTP 404
     */
    private ApiException managerNotFound(final UUID accountId) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.application_manager.not_found", Map.of("id", accountId.toString()))
        );
    }

    /**
     * Builds the error returned when the account is already a manager of the application.
     *
     * @param accountId the account identifier
     * @return an {@link ApiException} with HTTP 400
     */
    private ApiException managerAlreadyExists(final UUID accountId) {
        return new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.application_manager.already_exists", Map.of("id", accountId.toString()))
        );
    }

    /**
     * Builds the 404 {@link ApiException} of the application or the account whose foreign key the database rejected.
     * The foreign keys of {@code application_managers} are unnamed: PostgreSQL names them
     * {@code application_managers_<column>_fkey}.
     *
     * @param exception     the foreign key violation raised by the flush
     * @param applicationId the application identifier
     * @param accountId     the account identifier
     * @return the 404 exception of the missing reference, or {@code exception} when its constraint is not known
     */
    private RuntimeException managerReferenceNotFound(final DataIntegrityViolationException exception,
                                                      final UUID applicationId,
                                                      final UUID accountId) {
        return switch (violatedConstraint(exception)) {
            case "application_managers_app_id_fkey" -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.application.not_found", Map.of("id", applicationId.toString()))
            );
            case "application_managers_act_id_fkey" -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", accountId.toString()))
            );
            case null, default -> exception;
        };
    }
}
