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

import io.github.linagora.linid.im.api.model.superset.SupersetLoginRequest;
import io.github.linagora.linid.im.api.model.superset.SupersetLoginResponse;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Implementation of {@link SupersetCacheService} responsible for authenticating
 * against Superset and caching the resulting access token.
 *
 * <p>This class is isolated from {@code SupersetServiceImpl} so that the
 * {@code @Cacheable} annotation on {@link #getAccessToken()} is honored when
 * invoked from other beans. Spring's proxy-based caching is bypassed on
 * self-invocation (a call from within the same class via {@code this}), so
 * extracting the cached logic into its own Spring-managed bean ensures calls
 * always go through the proxy.</p>
 */
@Service
public class SupersetCacheServiceImpl implements SupersetCacheService {

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
     * Creates a Superset cache service.
     *
     * @param url          the base URL of the Superset instance
     * @param username     the username used to authenticate against Superset
     * @param password     the password used to authenticate against Superset
     */
    public SupersetCacheServiceImpl(@Value("${superset.url}") final String url,
                                    @Value("${superset.username}") final String username,
                                    @Value("${superset.password}") final String password) {
        this.username = username;
        this.password = password;
        this.restClient = RestClient.builder()
            .baseUrl(url)
            .build();
    }

    @Override
    @Cacheable(value = "superset-access-token", sync = true)
    public String getAccessToken() {
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

    @Override
    @CacheEvict(value = "superset-access-token", allEntries = true)
    public void clearCache() {
    }
}
