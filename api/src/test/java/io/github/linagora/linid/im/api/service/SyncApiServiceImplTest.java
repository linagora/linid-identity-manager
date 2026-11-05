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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.linagora.linid.im.api.model.sync.CycleStatusDTO;
import io.github.linagora.linid.im.api.model.sync.RecomputeRightsDTO;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Test class: SyncApiServiceImpl")
class SyncApiServiceImplTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    private MockRestServiceServer server;

    private SyncApiServiceImpl service;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://sync-api:8443");
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.service = new SyncApiServiceImpl(builder.build());
    }

    @Test
    @DisplayName("recomputeAccountRights should post the recomputation to sync-api and give its answer")
    void testRecomputeAccountRights() {
        server.expect(requestTo("https://sync-api:8443/accounts/" + ACCOUNT_ID + "/recompute-rights"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"alreadyRunning\":true}"));

        assertEquals(new RecomputeRightsDTO(true), service.recomputeAccountRights(ACCOUNT_ID));
        server.verify();
    }

    @Test
    @DisplayName("recomputeApplicationRights should post the recomputation to sync-api and give its answer")
    void testRecomputeApplicationRights() {
        server.expect(requestTo("https://sync-api:8443/applications/" + APPLICATION_ID + "/recompute-rights"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"alreadyRunning\":true}"));

        assertEquals(new RecomputeRightsDTO(true), service.recomputeApplicationRights(APPLICATION_ID));
        server.verify();
    }

    @Test
    @DisplayName("recomputeApplicationRights should raise a 503 ApiException when sync-api cannot be reached")
    void testRecomputeApplicationRights_unreachable() {
        server.expect(requestTo("https://sync-api:8443/applications/" + APPLICATION_ID + "/recompute-rights"))
            .andRespond(withException(new IOException("Connection refused")));

        var exception = assertThrows(ApiException.class, () -> service.recomputeApplicationRights(APPLICATION_ID));

        assertEquals(503, exception.getStatusCode());
        assertEquals("error.sync_api.unavailable", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeApplicationRights should raise a 503 ApiException when sync-api answers 503")
    void testRecomputeApplicationRights_tooManyWaiting() {
        server.expect(requestTo("https://sync-api:8443/applications/" + APPLICATION_ID + "/recompute-rights"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        var exception = assertThrows(ApiException.class, () -> service.recomputeApplicationRights(APPLICATION_ID));

        assertEquals(503, exception.getStatusCode());
        assertEquals("error.sync_api.unavailable", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeApplicationRights should raise a 502 ApiException when sync-api answers with an error")
    void testRecomputeApplicationRights_errorAnswered() {
        server.expect(requestTo("https://sync-api:8443/applications/" + APPLICATION_ID + "/recompute-rights"))
            .andRespond(withServerError());

        var exception = assertThrows(ApiException.class, () -> service.recomputeApplicationRights(APPLICATION_ID));

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeApplicationRights should raise a 502 ApiException when sync-api answers with an empty body")
    void testRecomputeApplicationRights_emptyBody() {
        server.expect(requestTo("https://sync-api:8443/applications/" + APPLICATION_ID + "/recompute-rights"))
            .andRespond(withStatus(HttpStatus.ACCEPTED));

        var exception = assertThrows(ApiException.class, () -> service.recomputeApplicationRights(APPLICATION_ID));

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }

    @Test
    @DisplayName("getCycleStatus should get the status of the cycle from sync-api")
    void testGetCycleStatus() {
        server.expect(requestTo("https://sync-api:8443/cycle"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"running\":true}", MediaType.APPLICATION_JSON));

        assertEquals(new CycleStatusDTO(true), service.getCycleStatus());
        server.verify();
    }

    @Test
    @DisplayName("recomputeAccountRights should raise a 503 ApiException when sync-api cannot be reached")
    void testRecomputeAccountRights_unreachable() {
        server.expect(requestTo("https://sync-api:8443/accounts/" + ACCOUNT_ID + "/recompute-rights"))
            .andRespond(withException(new IOException("Connection refused")));

        var exception = assertThrows(ApiException.class, () -> service.recomputeAccountRights(ACCOUNT_ID));

        assertEquals(503, exception.getStatusCode());
        assertEquals("error.sync_api.unavailable", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeAccountRights should raise a 502 ApiException when sync-api answers with an error")
    void testRecomputeAccountRights_errorAnswered() {
        server.expect(requestTo("https://sync-api:8443/accounts/" + ACCOUNT_ID + "/recompute-rights"))
            .andRespond(withServerError());

        var exception = assertThrows(ApiException.class, () -> service.recomputeAccountRights(ACCOUNT_ID));

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeAccountRights should raise a 503 ApiException when sync-api answers 503")
    void testRecomputeAccountRights_tooManyWaiting() {
        server.expect(requestTo("https://sync-api:8443/accounts/" + ACCOUNT_ID + "/recompute-rights"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        var exception = assertThrows(ApiException.class, () -> service.recomputeAccountRights(ACCOUNT_ID));

        assertEquals(503, exception.getStatusCode());
        assertEquals("error.sync_api.unavailable", exception.getError().key());
    }

    @Test
    @DisplayName("recomputeAccountRights should raise a 502 ApiException when sync-api answers with an empty body")
    void testRecomputeAccountRights_emptyBody() {
        server.expect(requestTo("https://sync-api:8443/accounts/" + ACCOUNT_ID + "/recompute-rights"))
            .andRespond(withStatus(HttpStatus.ACCEPTED));

        var exception = assertThrows(ApiException.class, () -> service.recomputeAccountRights(ACCOUNT_ID));

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }

    @Test
    @DisplayName("getCycleStatus should raise a 503 ApiException when sync-api cannot be reached")
    void testGetCycleStatus_unreachable() {
        server.expect(requestTo("https://sync-api:8443/cycle"))
            .andRespond(withException(new IOException("Connection refused")));

        var exception = assertThrows(ApiException.class, () -> service.getCycleStatus());

        assertEquals(503, exception.getStatusCode());
        assertEquals("error.sync_api.unavailable", exception.getError().key());
    }

    @Test
    @DisplayName("getCycleStatus should raise a 502 ApiException when sync-api rejects the client certificate")
    void testGetCycleStatus_errorAnswered() {
        server.expect(requestTo("https://sync-api:8443/cycle"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var exception = assertThrows(ApiException.class, () -> service.getCycleStatus());

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }

    @Test
    @DisplayName("getCycleStatus should raise a 502 ApiException when sync-api answers with an empty body")
    void testGetCycleStatus_emptyBody() {
        server.expect(requestTo("https://sync-api:8443/cycle"))
            .andRespond(withSuccess());

        var exception = assertThrows(ApiException.class, () -> service.getCycleStatus());

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.sync_api.bad_gateway", exception.getError().key());
    }
}
