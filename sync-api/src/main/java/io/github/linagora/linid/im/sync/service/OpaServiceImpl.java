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

package io.github.linagora.linid.im.sync.service;

import io.github.linagora.linid.im.sync.exception.OpaEvaluationException;
import io.github.linagora.linid.im.sync.exception.OpaUnreachableException;
import io.github.linagora.linid.im.sync.model.opa.OpaEvaluationResponse;
import io.github.linagora.linid.im.sync.model.opa.OpaResult;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Default implementation of {@link OpaService}, calling the OPA Data API.
 */
@Service
public class OpaServiceImpl implements OpaService {

    /**
     * REST client targeting the configured OPA server.
     */
    private final RestClient restClient;

    /**
     * Base URL of the OPA server, used to report the OPA server URL in the alerts.
     */
    private final String serverUrl;

    /**
     * OPA Data API path of the result of an application policy. The {@code {code}} placeholder is the application
     * code.
     */
    private final String evaluationPath;

    /**
     * Creates the service using the OPA {@link RestClient}, the OPA server URL and the evaluation path.
     *
     * @param opaRestClient  the {@link RestClient} targeting the OPA server
     * @param serverUrl      the OPA server base URL
     * @param evaluationPath the OPA Data API path (with a {@code {code}} placeholder)
     */
    public OpaServiceImpl(
        final RestClient opaRestClient,
        @Value("${opa.server-url}") final String serverUrl,
        @Value("${opa.evaluation-path}") final String evaluationPath) {
        this.restClient = opaRestClient;
        this.serverUrl = serverUrl;
        this.evaluationPath = evaluationPath;
    }

    @Override
    public OpaResult evaluate(final String applicationCode, final Map<String, Object> payload) {
        OpaEvaluationResponse response;
        try {
            response = restClient.post()
                .uri(evaluationPath, applicationCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("input", payload))
                .retrieve()
                .body(OpaEvaluationResponse.class);
        } catch (ResourceAccessException
                 | HttpServerErrorException.BadGateway
                 | HttpServerErrorException.ServiceUnavailable
                 | HttpServerErrorException.GatewayTimeout e) {
            AlertService.opaUnreachable("url=" + serverUrl, e);
            throw new OpaUnreachableException("Unable to reach the OPA server", e);
        } catch (RestClientException e) {
            throw new OpaEvaluationException("OPA failed to evaluate the policy of application " + applicationCode, e);
        }

        // OPA returns no result when the policy is missing or does not define a result.
        if (response == null || response.result() == null) {
            throw new OpaEvaluationException("OPA returned no result for the policy of application " + applicationCode);
        }
        return response.result();
    }
}
