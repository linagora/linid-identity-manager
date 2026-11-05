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

import io.github.linagora.linid.im.api.model.sync.CycleStatusDTO;
import io.github.linagora.linid.im.api.model.sync.RecomputeRightsDTO;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementation of {@link SyncApiService}, calling sync-api with the {@link RestClient} presenting the client
 * certificate of linid-api.
 *
 * <p>The errors of the calls are turned into {@link ApiException}: {@code 503} when sync-api cannot be reached, such
 * as a refused connection, a failed TLS handshake or a timeout, or when it answers {@code 503}, and {@code 502} when it
 * answers with another error status, an unreadable body or an empty body.</p>
 */
@Slf4j
@Service
public class SyncApiServiceImpl implements SyncApiService {

    /**
     * {@link RestClient} targeting sync-api.
     */
    private final RestClient restClient;

    /**
     * Creates the service using the sync-api {@link RestClient}.
     *
     * @param syncApiRestClient the {@link RestClient} targeting sync-api, with the client certificate
     */
    public SyncApiServiceImpl(final RestClient syncApiRestClient) {
        this.restClient = syncApiRestClient;
    }

    @Override
    public RecomputeRightsDTO recomputeAccountRights(final UUID id) {
        return call(() -> restClient.post()
            .uri("/accounts/{id}/recompute-rights", id)
            .retrieve()
            .body(RecomputeRightsDTO.class));
    }

    @Override
    public RecomputeRightsDTO recomputeApplicationRights(final UUID id) {
        return call(() -> restClient.post()
            .uri("/applications/{id}/recompute-rights", id)
            .retrieve()
            .body(RecomputeRightsDTO.class));
    }

    @Override
    public CycleStatusDTO getCycleStatus() {
        return call(() -> restClient.get()
            .uri("/cycle")
            .retrieve()
            .body(CycleStatusDTO.class));
    }

    /**
     * Sends a request to sync-api, turning its errors into {@link ApiException}, after logging their cause.
     *
     * @param request the request to sync-api, giving the body of its answer
     * @param <T>     the type of the body of the answer
     * @return the body of the answer
     * @throws ApiException with status {@code 503} when sync-api cannot be reached or answers {@code 503}, {@code 502}
     *     when it answers with another error or an empty body
     */
    private static <T> T call(final Supplier<T> request) {
        try {
            return Optional.ofNullable(request.get())
                .orElseThrow(() -> new RestClientException("Empty body answered by sync-api"));
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("sync-api is unavailable", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                I18nMessage.of("error.sync_api.unavailable"));
        } catch (RestClientException e) {
            log.error("Error answered by sync-api", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY.value(),
                I18nMessage.of("error.sync_api.bad_gateway"));
        }
    }
}
