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

import io.github.linagora.linid.im.sync.exception.OpaUnreachableException;
import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.model.opa.OpaResult;
import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default implementation of {@link RightsComputationService}: evaluates the policy with {@link OpaService} and renders
 * the claims template with {@link JinjaService}.
 *
 * <p>The claims template is rendered with the variables {@code payload}, the payload of the account, {@code allow} and
 * {@code roles}, returned by OPA. It is not validated when linid-api saves it: the rendered text must be a JSON object,
 * otherwise the pair is in error.</p>
 */
@Service
public class RightsComputationServiceImpl implements RightsComputationService {

    /**
     * Type of the claims, a JSON object.
     */
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    /**
     * Service evaluating the policies of the applications.
     */
    private final OpaService opaService;

    /**
     * Service rendering the claims templates.
     */
    private final JinjaService jinjaService;

    /**
     * Mapper parsing the rendered claims and serializing them as compact JSON.
     */
    private final JsonMapper jsonMapper;

    /**
     * Creates the service computing the rights of the pairs.
     *
     * @param opaService   the service evaluating the policies of the applications
     * @param jinjaService the service rendering the claims templates
     * @param jsonMapper   the mapper parsing and serializing the claims
     */
    public RightsComputationServiceImpl(final OpaService opaService, final JinjaService jinjaService,
                                        final JsonMapper jsonMapper) {
        this.opaService = opaService;
        this.jinjaService = jinjaService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ComputedRightRecord compute(final AccountComputedRightView pair, final Set<String> roleNames) {
        try {
            OpaResult result = opaService.evaluate(pair.getAppCode(), pair.getPayload());
            String claims = claims(pair, result);
            alertUnknownRoles(pair, result.roles(), roleNames);
            return new ComputedRightRecord(result.allow(), result.roles(), claims);
        } catch (OpaUnreachableException e) {
            // Concerns every pair: OpaService already logged the opaUnreachable alert, and the step fails
            throw e;
        } catch (RuntimeException e) {
            throw new RightsComputationException("Unable to compute the rights of the pair " + context(pair), e);
        }
    }

    /**
     * Renders the claims template of the application with the payload of the account and the OPA result, and parses it
     * as a JSON object.
     *
     * @param pair   the pair, with the payload of the account and the claims template of the application
     * @param result the OPA result of the pair
     * @return the claims, as compact JSON
     * @throws IllegalArgumentException when the rendered text is {@code null}, which Jackson parses without error
     */
    private String claims(final AccountComputedRightView pair, final OpaResult result) {
        Map<String, Object> context = Map.of(
            "payload", pair.getPayload(),
            "allow", result.allow(),
            "roles", result.roles());
        Map<String, Object> claims = jsonMapper.readValue(
            jinjaService.render(pair.getClaimsTemplate(), context), CLAIMS_TYPE);
        if (claims == null) {
            throw new IllegalArgumentException("The claims template is not rendered as a JSON object");
        }
        return jsonMapper.writeValueAsString(claims);
    }

    /**
     * Logs the {@code unknownRole} alert when OPA returned roles missing from the roles of the application.
     *
     * @param pair      the pair
     * @param roles     the roles returned by OPA
     * @param roleNames the names of the roles of the application
     */
    private static void alertUnknownRoles(final AccountComputedRightView pair, final List<String> roles,
                                          final Set<String> roleNames) {
        List<String> unknownRoles = roles.stream().filter(role -> !roleNames.contains(role)).toList();
        if (!unknownRoles.isEmpty()) {
            AlertService.unknownRole(context(pair) + ", roles=" + unknownRoles, null);
        }
    }

    /**
     * Identifies a pair in the alerts.
     *
     * @param pair the pair
     * @return the external identifier of the account and the code of the application
     */
    private static String context(final AccountComputedRightView pair) {
        return "account=" + pair.getExternalId() + ", application=" + pair.getAppCode();
    }
}
