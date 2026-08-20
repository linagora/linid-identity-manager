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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.linagora.linid.im.api.model.superset.SupersetAuthContext;
import io.github.linagora.linid.im.api.model.superset.SupersetCsrfTokenResponse;
import io.github.linagora.linid.im.api.model.superset.SupersetEmbeddedConfigResponse;
import io.github.linagora.linid.im.api.model.superset.SupersetGuestTokenRequest;
import io.github.linagora.linid.im.api.model.superset.SupersetLoginRequest;
import io.github.linagora.linid.im.api.model.superset.SupersetLoginResponse;
import io.github.linagora.linid.im.api.model.superset.SupersetRlsConfig;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenDTO;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.repository.AccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.github.linagora.linid.im.api.service.resolver.FieldValueResolver.getFieldAsString;

/**
 * Service implementation responsible for integrating LinID Identity Manager
 * with Apache Superset.
 *
 * <p>This service handles Superset authentication, CSRF and session management,
 * guest token generation, embedded dashboard configuration retrieval, and
 * Row Level Security (RLS) rule construction.</p>
 *
 * <p>RLS rules are built from the configured dashboard definitions and the
 * corresponding LinID entities used to resolve the values required by
 * Superset.</p>
 */
@Slf4j
@Service
public class SupersetServiceImpl implements SupersetService {

    /**
     * Repository used to retrieve account data required to build RLS rules.
     */
    private final AccountViewRepository accountViewRepository;

    /**
     * Repository used to retrieve organizational unit data required to build
     * RLS rules.
     */
    private final OrganizationalUnitViewRepository organizationalUnitViewRepository;

    /**
     * Base URL of the Superset instance.
     */
    private final String url;

    /**
     * Username used to authenticate against Superset.
     */
    private final String username;

    /**
     * Password used to authenticate against Superset.
     */
    private final String password;

    /**
     * HTTP client used to communicate with the Superset REST API.
     */
    private final RestClient restClient;

    /**
     * RLS configurations loaded from the configured YAML file.
     */
    private List<SupersetRlsConfig> rlsConfigs = List.of();

    /**
     * Creates a Superset service.
     *
     * @param url                              the base URL of the Superset instance
     * @param username                         the username used to authenticate against Superset
     * @param password                         the password used to authenticate against Superset
     * @param rlsConfigPath                    the path to the Superset RLS configuration file
     * @param accountViewRepository            repository used to retrieve account data for RLS rules
     * @param organizationalUnitViewRepository repository used to retrieve organizational unit data for RLS rules
     */
    public SupersetServiceImpl(@Value("${superset.url}") final String url,
                               @Value("${superset.username}") final String username,
                               @Value("${superset.password}") final String password,
                               @Value("${superset.rls-config}") final String rlsConfigPath,
                               final AccountViewRepository accountViewRepository,
                               final OrganizationalUnitViewRepository organizationalUnitViewRepository) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.restClient = RestClient.builder()
            .baseUrl(url)
            .build();
        this.accountViewRepository = accountViewRepository;
        this.organizationalUnitViewRepository = organizationalUnitViewRepository;

        initRlsConfiguration(rlsConfigPath);
    }

    /**
     * Loads and initializes the Superset RLS configuration from a YAML file.
     *
     * <p>The configuration is read from the {@code superset.dashboards}
     * section of the YAML document and converted into
     * {@link SupersetRlsConfig} instances.</p>
     *
     * <p>If the provided path is blank, no configuration is loaded.
     * An unreadable or invalid configuration file results in an internal
     * server error.</p>
     *
     * @param path the path to the YAML configuration file
     * @throws ApiException if the configuration file cannot be read
     */
    public void initRlsConfiguration(final String path) {
        if (StringUtils.isBlank(path)) {
            log.warn("Superset RLS configuration file is empty");
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

            JsonNode root = mapper.readTree(Path.of(path).toFile());

            rlsConfigs = mapper.convertValue(
                root.path("superset").path("dashboards"),
                new TypeReference<>() { }
            );
        } catch (IOException e) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                I18nMessage.of("error.superset.file", Map.of("path", path))
            );
        }

    }

    @Override
    public SupersetTokenDTO getToken(final UserPrincipal userPrincipal,
                                     final SupersetTokenRecord tokenRecord) {
        String accessToken = fetchAccessToken();
        var auth = fetchCsrfAndSession(accessToken);

        var guestTokenRequest = new SupersetGuestTokenRequest(
            new SupersetGuestTokenRequest.GuestUser(userPrincipal.getEmail(), "Guest", "User"),
            List.of(new SupersetGuestTokenRequest.Resource("dashboard", tokenRecord.dashboardId().toString())),
            buildRlsRules(userPrincipal, tokenRecord)
        );

        var response = restClient.post()
            .uri("/api/v1/security/guest_token/")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken())
            .header("X-CSRFToken", auth.csrfToken())
            .header(HttpHeaders.COOKIE, auth.sessionCookie())
            .header(HttpHeaders.REFERER, url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(guestTokenRequest)
            .retrieve()
            .body(SupersetTokenDTO.class);

        if (response == null || response.getToken() == null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.superset.guest_token", Map.of())
            );
        }

        return response;
    }

    /**
     * Authenticates against Superset and retrieves an access token.
     * <p>
     * The result is cached under {@code superset-access-token} to avoid
     * re-authenticating on every call. The cache is configured with a single
     * entry ({@code maximumSize=1}) and a TTL slightly shorter than Superset's
     * {@code JWT_ACCESS_TOKEN_EXPIRES} (default: 55 minutes) to renew the token
     * before it expires server-side.
     * <p>
     * Concurrent calls are synchronized ({@code sync = true}) so that only one
     * thread triggers the Superset login request when the cache is empty or
     * expired; other threads wait for and reuse that result instead of issuing
     * duplicate login requests.
     *
     * @return the Superset access token
     * @throws ApiException if Superset does not return an access token
     */
    @Cacheable(value = "superset-access-token", sync = true)
    public String fetchAccessToken() {
        var loginRequest = new SupersetLoginRequest(username, password, "db", true);

        var response = restClient.post()
            .uri("/api/v1/security/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .retrieve()
            .body(SupersetLoginResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY.value(),
                I18nMessage.of("error.superset.access_token", Map.of())
            );
        }

        return response.accessToken();
    }

    /**
     * Retrieves the CSRF token and session cookie required for authenticated
     * Superset API requests.
     *
     * @param accessToken the Superset access token used for authentication
     * @return the authentication context containing the access token, CSRF token,
     *         and session cookie
     * @throws ApiException if the CSRF token or session cookie cannot be retrieved
     */
    public SupersetAuthContext fetchCsrfAndSession(final String accessToken) {
        ResponseEntity<SupersetCsrfTokenResponse> response = restClient.get()
            .uri("/api/v1/security/csrf_token/")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .toEntity(SupersetCsrfTokenResponse.class);

        String csrfToken = Optional.ofNullable(response.getBody())
            .map(SupersetCsrfTokenResponse::result)
            .filter(token -> !token.isBlank())
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.BAD_GATEWAY.value(),
                    I18nMessage.of("error.superset.csrf_token", Map.of())
                )
            );

        String sessionCookie = response.getHeaders()
            .getOrEmpty(HttpHeaders.SET_COOKIE)
            .stream()
            .filter(cookie -> cookie.startsWith("session="))
            .map(cookie -> cookie.split(";", 2)[0])
            .findFirst()
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.BAD_GATEWAY.value(),
                    I18nMessage.of("error.superset.session_cookie", Map.of())
                )
            );

        return new SupersetAuthContext(
            accessToken,
            csrfToken,
            sessionCookie
        );
    }

    @Override
    public UUID getDashboardId(final String slug) {
        String accessToken = fetchAccessToken();

        var response = restClient.get()
            .uri("/api/v1/dashboard/{slug}/embedded", slug)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(SupersetEmbeddedConfigResponse.class);

        return Optional.ofNullable(response)
            .map(SupersetEmbeddedConfigResponse::result)
            .map(SupersetEmbeddedConfigResponse.Result::uuid)
            .map(UUID::fromString)
            .orElseThrow(() ->
                new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    I18nMessage.of("error.superset.slug_configuration", Map.of())
                )
            );
    }

    /**
     * Builds the Superset RLS rules applicable to a dashboard.
     *
     * <p>Only configurations matching the requested dashboard slug and having
     * RLS enabled are used.</p>
     *
     * @param userPrincipal the authenticated LinID user
     * @param tokenRecord   the guest token request containing the dashboard slug
     *                      and RLS identifier
     * @return the RLS rules to apply to the Superset guest token
     */
    public List<SupersetGuestTokenRequest.RlsRule> buildRlsRules(final UserPrincipal userPrincipal,
                                                                 final SupersetTokenRecord tokenRecord) {
        return rlsConfigs.stream()
            .filter((rlsConfig) -> rlsConfig.slug().equals(tokenRecord.dashboardSlug()))
            .filter(SupersetRlsConfig::rlsEnabled)
            .map((rlsConfig) -> buildRlsRule(userPrincipal, tokenRecord, rlsConfig))
            .toList();
    }

    /**
     * Builds a Superset RLS rule from a dashboard RLS configuration.
     *
     * <p>The configured entity determines which repository is used to resolve
     * the RLS value. The configured attribute identifies the field whose value
     * is used in the generated Superset RLS clause.</p>
     *
     * @param userPrincipal the authenticated LinID user
     * @param tokenRecord   the guest token request containing the RLS identifier
     * @param rlsConfig     the RLS configuration associated with the dashboard
     * @return the generated Superset RLS rule
     * @throws ApiException if the configured entity is not supported or the
     *                      referenced entity value cannot be resolved
     */
    public SupersetGuestTokenRequest.RlsRule buildRlsRule(final UserPrincipal userPrincipal,
                                                          final SupersetTokenRecord tokenRecord,
                                                          final SupersetRlsConfig rlsConfig) {
        String value;

        if (rlsConfig.entity().equalsIgnoreCase("ACCOUNT")) {
            value = getAccountValue(tokenRecord.rlsId(), rlsConfig.attribute());
        } else if (rlsConfig.entity().equalsIgnoreCase("ORGANIZATIONAL_UNIT")) {
            value = getOrganizationalUnitValue(tokenRecord.rlsId(), rlsConfig.attribute());
        } else {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.entity.unknown", Map.of("entity", rlsConfig.entity()))
            );
        }

        String clause = String.format("rls_id='%s'", value);

        return new SupersetGuestTokenRequest.RlsRule(clause, rlsConfig.datasetId());
    }

    /**
     * Retrieves an account attribute value used to build an RLS rule.
     *
     * @param rlsId     the account identifier
     * @param attribute the account field name to retrieve
     * @return the value of the requested account attribute as a string
     * @throws ApiException if the account does not exist or the requested
     *                      attribute is unknown
     */
    public String getAccountValue(final String rlsId, final String attribute) {
        var account = accountViewRepository.findById(UUID.fromString(rlsId))
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.account.not_found", Map.of("id", rlsId))
            ));

        return getFieldAsString(account, attribute, "account");
    }

    /**
     * Retrieves an organizational unit attribute value used to build an RLS rule.
     *
     * @param rlsId     the organizational unit identifier
     * @param attribute the organizational unit field name to retrieve
     * @return the value of the requested organizational unit attribute as a string
     * @throws ApiException if the organizational unit does not exist or the
     *                      requested attribute is unknown
     */
    public String getOrganizationalUnitValue(final String rlsId, final String attribute) {
        var account = organizationalUnitViewRepository.findById(UUID.fromString(rlsId))
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of("error.organizational.unit.not_found", Map.of("id", rlsId))
            ));

        return getFieldAsString(account, attribute, "organizational-unit");

    }
}
