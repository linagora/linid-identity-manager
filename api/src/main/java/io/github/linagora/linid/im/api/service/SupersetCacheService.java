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

/**
 * Service responsible for providing and caching the Superset access token
 * used to authenticate LinID Identity Manager against the Superset REST API.
 *
 * <p>This service isolates the token acquisition and caching logic so that
 * it can be safely invoked from other Spring-managed beans without running
 * into the Spring AOP self-invocation limitation, where {@code @Cacheable}
 * is silently ignored when called from within the same class.</p>
 */
public interface SupersetCacheService {

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
     */
    String getAccessToken();

    /**
     * Evicts the cached Superset access token.
     *
     * <p>This forces the next call to {@link #getAccessToken()} to
     * re-authenticate against Superset instead of reusing a stale or
     * rejected token. It is typically invoked after Superset responds with
     * an authentication failure (e.g. {@code 401 Unauthorized} or
     * {@code 403 Forbidden}), indicating that the cached token is no longer
     * valid.</p>
     */
    void clearCache();
}
