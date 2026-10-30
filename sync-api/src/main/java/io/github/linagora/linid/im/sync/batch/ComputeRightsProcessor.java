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

package io.github.linagora.linid.im.sync.batch;

import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.ApplicationRole;
import io.github.linagora.linid.im.sync.persistence.model.ComputedRight;
import io.github.linagora.linid.im.sync.persistence.model.PairChecksums;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ApplicationRoleRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ComputedRightRepository;
import io.github.linagora.linid.im.sync.service.RightsComputationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Processor of the steps computing the rights: computes the rights of an account and application pair with
 * {@link RightsComputationService}, and gives the row of {@code computed_rights} to write.
 *
 * <p>The role names of an application are read at its first pair in the step, and kept until its end. The processor
 * is step-scoped, so that each step, including the recomputations running at the same time, has its own cache.</p>
 *
 * <p>A pair whose payload, script or claims template checksum changed since it was read is filtered out, as well as a
 * pair no longer in the view, its application being no longer deployed: a more recent processing already wrote it, or
 * it stays {@code to_compute} and is computed again. Both are counted in the filter count of the step. A pair in error
 * raises a {@link io.github.linagora.linid.im.sync.exception.RightsComputationException}, skipped by the step, which
 * logs the {@code pairError} alert: the pair keeps its previous row, if any.</p>
 */
@Component
@StepScope
public class ComputeRightsProcessor implements ItemProcessor<AccountComputedRightView, ComputedRight> {

    /**
     * Service computing the rights of the pairs.
     */
    private final RightsComputationService rightsComputationService;

    /**
     * Repository of {@code application_roles}.
     */
    private final ApplicationRoleRepository applicationRoleRepository;

    /**
     * Repository of {@code account_computed_rights_view}, giving the current checksums of the pairs.
     */
    private final AccountComputedRightViewRepository viewRepository;

    /**
     * Repository of {@code computed_rights}, holding the current rights of the pairs.
     */
    private final ComputedRightRepository computedRightRepository;

    /**
     * Identifier of the technical account of {@code sync-api}, the author of the rows written.
     */
    private final UUID technicalAccountId;

    /**
     * Identifier of the {@code sync-api} instance, written with the rights.
     */
    private final String instanceId;

    /**
     * Role names of the applications whose pairs were processed during the step, by application.
     */
    private final Map<UUID, Set<String>> roleNamesByApplication = new HashMap<>();

    /**
     * Creates the processor of the rights.
     *
     * @param rightsComputationService  the service computing the rights of the pairs
     * @param applicationRoleRepository the repository of {@code application_roles}
     * @param viewRepository            the repository of {@code account_computed_rights_view}
     * @param computedRightRepository   the repository of {@code computed_rights}
     * @param technicalAccountId        the identifier of the technical account of {@code sync-api}
     * @param instanceId                the identifier of the {@code sync-api} instance
     */
    public ComputeRightsProcessor(
        final RightsComputationService rightsComputationService,
        final ApplicationRoleRepository applicationRoleRepository,
        final AccountComputedRightViewRepository viewRepository,
        final ComputedRightRepository computedRightRepository,
        @Value("${sync.technical-account-id}") final UUID technicalAccountId,
        @Value("${sync.instance-id}") final String instanceId) {
        this.rightsComputationService = rightsComputationService;
        this.applicationRoleRepository = applicationRoleRepository;
        this.viewRepository = viewRepository;
        this.computedRightRepository = computedRightRepository;
        this.technicalAccountId = technicalAccountId;
        this.instanceId = instanceId;
    }

    /**
     * Computes the rights of a pair, and gives its row when the pair did not change since it was read: its current row
     * updated, or a new row when the pair was never computed.
     *
     * @param item the pair, with the checksums read with it
     * @return the row to write, {@code null} when the pair changed or is no longer in the view
     * @throws io.github.linagora.linid.im.sync.exception.RightsComputationException when the rights of the pair cannot
     *     be computed
     * @throws io.github.linagora.linid.im.sync.exception.OpaUnreachableException when the OPA server cannot be
     *     reached
     */
    @Override
    public ComputedRight process(final AccountComputedRightView item) {
        ComputedRightRecord rights = rightsComputationService.compute(item, roleNames(item.getAppId()));

        // Pair changed or no longer deployed since it was read: its rights are outdated
        Optional<PairChecksums> current = viewRepository.findChecksumsByActIdAndAppId(item.getActId(), item.getAppId());
        if (current.isEmpty() || !hasSameChecksums(item, current.get())) {
            return null;
        }

        ComputedRight row = computedRightRepository.findByActIdAndAppId(item.getActId(), item.getAppId())
            .orElseGet(() -> newRow(item));
        row.setAllow(rights.allow());
        row.setRoles(rights.roles());
        row.setClaims(rights.claims());
        row.setPayloadChecksum(item.getPayloadChecksum());
        row.setScriptChecksum(item.getScriptChecksum());
        row.setClaimsTemplateChecksum(item.getClaimsTemplateChecksum());
        row.setSyncInstanceId(instanceId);
        row.setSyncJobExecutionId(StepSynchronizationManager.getContext().getStepExecution().getJobExecutionId());
        row.setUpdatedBy(technicalAccountId);
        return row;
    }

    /**
     * Gives the role names of an application, read at its first pair in the step.
     *
     * @param appId the identifier of the application
     * @return the names of the roles of the application
     */
    private Set<String> roleNames(final UUID appId) {
        return roleNamesByApplication.computeIfAbsent(appId, id -> applicationRoleRepository.findAllByAppId(id)
            .stream()
            .map(ApplicationRole::getName)
            .collect(Collectors.toSet()));
    }

    /**
     * Tells whether the checksums of the payload, the script and the claims template read with a pair are still the
     * current ones.
     *
     * @param pair    the pair, as read before its computation
     * @param current the current checksums of the pair
     * @return {@code true} when the three checksums are unchanged
     */
    private static boolean hasSameChecksums(final AccountComputedRightView pair, final PairChecksums current) {
        return Objects.equals(pair.getPayloadChecksum(), current.payloadChecksum())
            && Objects.equals(pair.getScriptChecksum(), current.scriptChecksum())
            && Objects.equals(pair.getClaimsTemplateChecksum(), current.claimsTemplateChecksum());
    }

    /**
     * Creates the row of a pair never computed, with the technical account as author.
     *
     * @param pair the pair
     * @return the new row
     */
    private ComputedRight newRow(final AccountComputedRightView pair) {
        ComputedRight row = new ComputedRight();
        row.setActId(pair.getActId());
        row.setAppId(pair.getAppId());
        row.setCreatedBy(technicalAccountId);
        return row;
    }
}
