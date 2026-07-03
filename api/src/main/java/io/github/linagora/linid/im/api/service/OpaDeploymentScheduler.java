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

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically deploys to OPA every application whose policy is pending deployment.
 *
 * <p>Each application is deployed in its own transaction through the {@link OpaApplicationDeployer}, so that a failure
 * on one application neither rolls back the deployment of the others nor blocks the batch.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpaDeploymentScheduler {

    /**
     * Repository used to list the applications pending deployment.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Deployer publishing a single application in its own transaction.
     */
    private final OpaApplicationDeployer opaApplicationDeployer;

    /**
     * Deploys all applications that are pending deployment to OPA.
     *
     * <p>Retrieves every application with a generated script that has not been deployed yet and deploys each one
     * independently. A deployment failure on one application is logged and does not block the processing of the
     * others; it will simply be retried on the next execution.</p>
     */
    @Scheduled(fixedDelayString = "${opa.deployment.interval:5m}")
    public void deployPendingApplications() {
        var applications = applicationRepository.findByDeployedAtIsNullAndScriptIsNotNull();

        if (applications.isEmpty()) {
            log.debug("No application pending OPA deployment");
            return;
        }

        log.info("Deploying {} application(s) to OPA", applications.size());

        for (Application application : applications) {
            deploySafely(application);
        }
    }

    /**
     * Deploys a single application, isolating any failure so it does not block the other applications.
     *
     * @param application the application to deploy
     */
    private void deploySafely(final Application application) {
        try {
            opaApplicationDeployer.deploy(application);
            log.info("Successfully deployed policy of application {} to OPA", application.getCode());
        } catch (RuntimeException e) {
            log.error("Failed to deploy policy of application {} to OPA, it will be retried on the next execution",
                application.getCode(), e);
        }
    }
}
