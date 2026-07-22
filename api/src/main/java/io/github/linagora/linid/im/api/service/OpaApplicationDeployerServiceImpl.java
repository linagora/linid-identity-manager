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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Deploys a single application policy to OPA within its own transaction.
 *
 * <p>Extracted into a dedicated bean so that {@link Transactional} is honored
 * (it would be ignored on a self-invoked private method). Each application is therefore committed independently: a
 * failure on one application never rolls back the {@code deployedAt} update of another that was already deployed.</p>
 */
@Service
@Transactional(propagation = REQUIRES_NEW)
@RequiredArgsConstructor
public class OpaApplicationDeployerServiceImpl implements OpaApplicationDeployerService {

    /**
     * Repository used to persist the application deployment status.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Service publishing the policy to the OPA server.
     */
    private final OpaService opaService;

    /**
     * Service used to retrieve the application to deploy.
     */
    private final ApplicationService applicationService;


    @Override
    public Application deploy(final Application application) {
        if (application.getScript() == null || application.getScript().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), I18nMessage.of("error.application.script"
                    + ".missing", Map.of("applicationId", application.getId().toString())));
        }
        var deployedDate = opaService.publish(application);
        application.setDeployedAt(deployedDate);
        return applicationRepository.save(application);
    }

    @Override
    public Application deploy(final UserPrincipal userPrincipal, final UUID id, final boolean force) {
        var application = applicationService.findById(userPrincipal, id);
        if (!force && application.getDeployedAt() != null) {
            return application;
        }
        return deploy(application);
    }
}
