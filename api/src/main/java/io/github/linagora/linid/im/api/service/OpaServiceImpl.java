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
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Default implementation of {@link OpaService}.
 *
 * <p>Encapsulates the OPA workflow: rendering a policy from the active rules of an application through the
 * {@link JinjaService} and publishing it to the OPA server.</p>
 */
@Slf4j
@Service
public class OpaServiceImpl implements OpaService {

    /**
     * Service rendering the policy template.
     */
    private final JinjaService jinjaService;

    /**
     * REST client targeting the configured OPA server.
     */
    private final RestClient restClient;

    /**
     * Classpath (or file) location of the OPA policy template.
     */
    private final Resource scriptTemplate;

    /**
     * OPA REST Policy API path used to publish a policy. The {@code {id}} placeholder is the policy identifier.
     */
    private final String policyPath;

    /**
     * Creates the service using the OPA {@link RestClient}, the policy template location and the policy path.
     *
     * @param jinjaService   the generic Jinja rendering service
     * @param opaRestClient  the {@link RestClient} targeting the OPA server
     * @param scriptTemplate the OPA policy template resource
     * @param policyPath     the OPA policy API path (with an {@code {id}} placeholder)
     */
    public OpaServiceImpl(
        final JinjaService jinjaService,
        final RestClient opaRestClient,
        @Value("${opa.script.location}") final Resource scriptTemplate,
        @Value("${opa.script.policy-path}") final String policyPath) {
        this.jinjaService = jinjaService;
        this.restClient = opaRestClient;
        this.scriptTemplate = scriptTemplate;
        this.policyPath = policyPath;
    }

    @Override
    public String generate(final Application application, final List<ApplicationRule> activeRules) {
        List<String> fragments = activeRules.stream()
            .sorted(Comparator.comparing(ApplicationRule::getPriority))
            .map(ApplicationRule::getScript)
            .toList();

        Map<String, Object> context = new HashMap<>();
        context.put("application", Map.of("code", application.getCode()));
        context.put("fragments", fragments);

        return jinjaService.render(loadTemplate(), context);
    }

    @Override
    public OffsetDateTime publish(final Application application) {
        try {
            restClient.put()
                .uri(policyPath, application.getCode())
                .contentType(MediaType.TEXT_PLAIN)
                .body(application.getScript())
                .retrieve()
                .toBodilessEntity();
            return OffsetDateTime.now();
        } catch (RestClientException e) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY.value(),
                I18nMessage.of("error.opa.policy.publish_failed", Map.of("policyId", application.getCode())),
                e
            );
        }
    }

    /**
     * Loads the OPA policy template content from the configured location.
     *
     * @return the raw template content
     */
    private String loadTemplate() {
        try {
            return StreamUtils.copyToString(scriptTemplate.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                I18nMessage.of("error.opa.template.not_readable",
                    Map.of("location", String.valueOf(scriptTemplate.getDescription()))),
                e
            );
        }
    }
}
