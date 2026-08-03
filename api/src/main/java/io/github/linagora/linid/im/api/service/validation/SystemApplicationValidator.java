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

package io.github.linagora.linid.im.api.service.validation;

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the immutability rules protecting the system-reserved application.
 *
 * <p>The application identified by the {@link #SYSTEM_APPLICATION_CODE} code is seeded by a Flyway migration
 * and must never be updated or deleted, nor may its roles be created, updated or deleted. Each rule is exposed
 * as a public {@code ensureXxx} method so it can be tested in isolation and reused by the services owning the
 * mutation operations.</p>
 */
@Component
@RequiredArgsConstructor
public class SystemApplicationValidator {

    /**
     * Functional code of the system-reserved application, seeded by the Flyway migration.
     */
    public static final String SYSTEM_APPLICATION_CODE = "LINID";

    /**
     * Repository used to resolve the code of the application targeted by a mutation.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Rejects any mutation targeting the system-reserved application itself.
     *
     * @param application the application targeted by the mutation
     * @throws ApiException if the application is the system-reserved one (HTTP 400)
     */
    public void ensureApplicationIsMutable(final Application application) {
        if (!isSystemApplication(application)) {
            return;
        }

        throw new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.application.system_reserved", Map.of("code", SYSTEM_APPLICATION_CODE))
        );
    }

    /**
     * Rejects any mutation targeting a role of the system-reserved application.
     *
     * <p>The application is resolved from its identifier: an unknown identifier is not this validator's
     * concern and is left to the caller, which reports it as a 404.</p>
     *
     * @param applicationId the identifier of the application owning the targeted role
     * @throws ApiException if the application is the system-reserved one (HTTP 400)
     */
    public void ensureRolesAreMutable(final UUID applicationId) {
        var isSystemApplication = applicationRepository.findById(applicationId)
            .map(this::isSystemApplication)
            .orElse(false);

        if (!isSystemApplication) {
            return;
        }

        throw new ApiException(
            HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.application_role.system_reserved", Map.of("code", SYSTEM_APPLICATION_CODE))
        );
    }

    /**
     * Tells whether the given application is the system-reserved one.
     *
     * @param application the application to check
     * @return {@code true} when the application carries the system-reserved code
     */
    private boolean isSystemApplication(final Application application) {
        return SYSTEM_APPLICATION_CODE.equals(application.getCode());
    }
}
