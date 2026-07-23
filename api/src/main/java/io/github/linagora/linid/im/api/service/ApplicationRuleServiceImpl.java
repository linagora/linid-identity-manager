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

import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleMapper;
import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link ApplicationRuleService}.
 *
 * <p>Enforces the existence of the owning application and the uniqueness of the rule {@code code} within
 * that application. The {@code scriptChecksum} is computed from the {@code script}; rules are always
 * created disabled. Regenerating the owning application OPA policy after a rule change is orchestrated by the
 * {@code ApplicationRuleController}, not by this service.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationRuleServiceImpl implements ApplicationRuleService {

    /**
     * Repository used to manage {@link ApplicationRule} persistence operations.
     */
    private final ApplicationRuleRepository applicationRuleRepository;

    /**
     * Repository used to manage {@link ApplicationRuleView} persistence operations.
     */
    private final ApplicationRuleViewRepository applicationRuleViewRepository;

    /**
     * Repository used to check the existence of the owning application.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Mapper used to convert application rule entities and DTOs.
     */
    private final ApplicationRuleMapper mapper;

    /**
     * Service used to compute the script checksum.
     */
    private final ChecksumService checksumService;

    @Override
    public ApplicationRule create(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final ApplicationRuleRecord rule) {
        ensureApplicationExists(applicationId);

        if (applicationRuleRepository.existsByApplicationIdAndCode(applicationId, rule.code())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.application_rule.code.already_exists", Map.of("code", rule.code()))
            );
        }

        var entity = mapper.toEntity(rule, userPrincipal);
        entity.setApplicationId(applicationId);
        entity.setDisabled(true);
        entity.setScriptChecksum(checksumService.compute(rule.script()));

        return applicationRuleRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationRuleView> findAll(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final ApplicationRuleViewQueryFilterDto filters,
        final Pageable pageable) {
        ensureApplicationExists(applicationId);

        Specification<ApplicationRuleView> specification =
            new SpringQueryFilterSpecification<>(ApplicationRuleView.class, filters)
                .and(new SpringQueryFilterSpecification<>(ApplicationRuleView.class, Map.of(
                    "applicationId", List.of(applicationId.toString())
                )));

        return applicationRuleViewRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationRule findById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        return applicationRuleRepository.findByIdAndApplicationId(id, applicationId)
            .orElseThrow(() -> ruleNotFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationRuleView findViewById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        return applicationRuleViewRepository.findByIdAndApplicationId(id, applicationId)
            .orElseThrow(() -> ruleNotFound(id));
    }

    @Override
    public ApplicationRule update(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id,
        final ApplicationRuleRecord rule) {
        var entity = findById(userPrincipal, applicationId, id);

        if (!rule.code().equals(entity.getCode())
            && applicationRuleRepository.existsByApplicationIdAndCodeAndIdNot(applicationId, rule.code(), id)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.application_rule.code.already_exists", Map.of("code", rule.code()))
            );
        }

        if (!rule.script().equals(entity.getScript())) {
            entity.setScriptChecksum(checksumService.compute(rule.script()));
        }

        entity.setCode(rule.code());
        entity.setDescription(rule.description());
        entity.setPriority(rule.priority());
        entity.setScript(rule.script());
        entity.setDisabled(rule.disabled());
        entity.setUpdatedBy(userPrincipal.getId());

        return applicationRuleRepository.save(entity);
    }

    @Override
    public void deleteById(
        final UserPrincipal userPrincipal,
        final UUID applicationId,
        final UUID id) {
        var entity = findById(userPrincipal, applicationId, id);

        applicationRuleRepository.delete(entity);
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
     * Builds the 404 {@link ApiException} for a missing rule.
     *
     * @param id the rule identifier
     * @return the exception to throw
     */
    private ApiException ruleNotFound(final UUID id) {
        return new ApiException(
            HttpStatus.NOT_FOUND.value(),
            I18nMessage.of("error.application_rule.not_found", Map.of("id", id.toString()))
        );
    }
}
