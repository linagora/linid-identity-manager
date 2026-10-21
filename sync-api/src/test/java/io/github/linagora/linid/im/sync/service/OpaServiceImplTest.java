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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.exception.OpaEvaluationException;
import io.github.linagora.linid.im.sync.exception.OpaUnreachableException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Test class: OpaServiceImpl")
class OpaServiceImplTest {

    private static final String SERVER_URL = "http://opa:8181";

    private static final String URL = SERVER_URL + "/v1/data/authz/payroll/result";

    private static final Map<String, Object> PAYLOAD = Map.of("email", "john@example.com", "groups", List.of("hr"));

    private MockRestServiceServer server;

    private OpaServiceImpl service;

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private ListAppender<ILoggingEvent> alertAppender;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(SERVER_URL);
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.service = new OpaServiceImpl(builder.build(), SERVER_URL, "/v1/data/authz/{code}/result");
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("evaluate should POST the unchanged payload as the OPA input and return the allow and the roles")
    void testEvaluate() {
        server.expect(requestTo(URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"input\":{\"email\":\"john@example.com\",\"groups\":[\"hr\"]}}",
                JsonCompareMode.STRICT))
            .andRespond(withSuccess(
                "{\"result\":{\"allow\":true,\"roles\":[\"admin\",\"user\"]}}", MediaType.APPLICATION_JSON));

        var result = service.evaluate("payroll", PAYLOAD);

        assertTrue(result.allow());
        assertEquals(List.of("admin", "user"), result.roles());
        server.verify();
    }

    @Test
    @DisplayName("evaluate should return a denied access without roles when the policy denies it")
    void testEvaluate_denied() {
        server.expect(requestTo(URL))
            .andRespond(withSuccess("{\"result\":{\"allow\":false,\"roles\":[]}}", MediaType.APPLICATION_JSON));

        var result = service.evaluate("payroll", PAYLOAD);

        assertFalse(result.allow());
        assertEquals(List.of(), result.roles());
        server.verify();
    }

    @Test
    @DisplayName("evaluate should raise an OpaEvaluationException when the policy is missing")
    void testEvaluate_missingPolicy() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(OpaEvaluationException.class, () -> service.evaluate("payroll", PAYLOAD));
        server.verify();
    }

    @Test
    @DisplayName("evaluate should raise an OpaEvaluationException when the response has no body")
    void testEvaluate_emptyResponse() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        assertThrows(OpaEvaluationException.class, () -> service.evaluate("payroll", PAYLOAD));
        server.verify();
    }

    @Test
    @DisplayName("evaluate should raise an OpaEvaluationException when OPA rejects the input")
    void testEvaluate_badRequest() {
        server.expect(requestTo(URL)).andRespond(withBadRequest());

        assertThrows(OpaEvaluationException.class, () -> service.evaluate("payroll", PAYLOAD));
        server.verify();
    }

    @Test
    @DisplayName("evaluate should raise an OpaEvaluationException, without the opaUnreachable alert, when OPA fails to "
        + "evaluate the policy")
    void testEvaluate_serverError() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(OpaEvaluationException.class, () -> service.evaluate("payroll", PAYLOAD));
        assertTrue(alertAppender.list.isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("evaluate should log the opaUnreachable alert and raise an OpaUnreachableException when the OPA "
        + "server cannot be reached")
    void testEvaluate_unreachable() {
        server.expect(requestTo(URL)).andRespond(withException(new ConnectException("Connection refused")));

        assertThrows(OpaUnreachableException.class, () -> service.evaluate("payroll", PAYLOAD));
        assertEquals(1, alertAppender.list.size());
        assertEquals(Level.ERROR, alertAppender.list.getFirst().getLevel());
        assertEquals("OPA unreachable, authorization check aborted — url=" + SERVER_URL,
            alertAppender.list.getFirst().getFormattedMessage());
        server.verify();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = HttpStatus.class, names = {"BAD_GATEWAY", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"})
    @DisplayName("evaluate should log the opaUnreachable alert and raise an OpaUnreachableException when a gateway in "
        + "front of OPA answers 502, 503 or 504")
    void testEvaluate_gatewayError(final HttpStatus status) {
        server.expect(requestTo(URL)).andRespond(withStatus(status));

        assertThrows(OpaUnreachableException.class, () -> service.evaluate("payroll", PAYLOAD));
        assertEquals(1, alertAppender.list.size());
        assertEquals(Level.ERROR, alertAppender.list.getFirst().getLevel());
        assertEquals("OPA unreachable, authorization check aborted — url=" + SERVER_URL,
            alertAppender.list.getFirst().getFormattedMessage());
        server.verify();
    }
}
