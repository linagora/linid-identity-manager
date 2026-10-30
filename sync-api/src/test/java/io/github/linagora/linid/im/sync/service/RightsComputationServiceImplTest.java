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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.exception.OpaEvaluationException;
import io.github.linagora.linid.im.sync.exception.OpaUnreachableException;
import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.model.opa.OpaResult;
import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: RightsComputationServiceImpl")
class RightsComputationServiceImplTest {

    private static final String APPLICATION_CODE = "APP";

    private static final String CLAIMS_TEMPLATE =
        "{\"sub\": {{ payload.external_id | tojson }}, \"roles\": {{ roles | tojson }}}";

    private static final Map<String, Object> PAYLOAD = Map.of("external_id", "jdoe");

    private static final Set<String> ROLE_NAMES = Set.of("Administrator", "Reader");

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    @Mock
    private OpaService opaService;

    @Mock
    private JinjaService jinjaService;

    private ListAppender<ILoggingEvent> alertAppender;

    private RightsComputationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RightsComputationServiceImpl(opaService, jinjaService, JsonMapper.builder().build());
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("compute should give the OPA result and the claims rendered with the payload, the allow and the "
        + "roles, as compact JSON, without alert")
    void testCompute() {
        var pair = pair();
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenReturn(new OpaResult(true, List.of("Administrator")));
        when(jinjaService.render(CLAIMS_TEMPLATE,
            Map.of("payload", PAYLOAD, "allow", true, "roles", List.of("Administrator"))))
            .thenReturn("{ \"sub\": \"jdoe\", \"roles\": [\"Administrator\"] }\n");

        var result = service.compute(pair, ROLE_NAMES);

        assertEquals(new ComputedRightRecord(true, List.of("Administrator"),
            "{\"sub\":\"jdoe\",\"roles\":[\"Administrator\"]}"), result);
        assertTrue(alertAppender.list.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("validResults")
    @DisplayName("compute should give the result of a pair allowed without role, or not allowed with roles")
    void testCompute_validResults(final boolean allow, final List<String> roles) {
        var pair = pair();
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenReturn(new OpaResult(allow, roles));
        when(jinjaService.render(CLAIMS_TEMPLATE, Map.of("payload", PAYLOAD, "allow", allow, "roles", roles)))
            .thenReturn("{\"sub\": \"jdoe\"}");

        assertEquals(new ComputedRightRecord(allow, roles, "{\"sub\":\"jdoe\"}"), service.compute(pair, ROLE_NAMES));
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("compute should raise the unknownRole alert with the roles missing from the roles of the application, "
        + "and give the result anyway")
    void testCompute_unknownRoles() {
        var pair = pair();
        var roles = List.of("Administrator", "Auditor", "Ghost");
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenReturn(new OpaResult(true, roles));
        when(jinjaService.render(eq(CLAIMS_TEMPLATE), any())).thenReturn("{\"sub\": \"jdoe\"}");

        assertEquals(new ComputedRightRecord(true, roles, "{\"sub\":\"jdoe\"}"), service.compute(pair, ROLE_NAMES));

        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.WARN, alert.getLevel());
        assertEquals("Unknown role returned by OPA, missing from application_roles — account=jdoe, application=APP, "
            + "roles=[Auditor, Ghost]", alert.getFormattedMessage());
    }

    @Test
    @DisplayName("compute should throw a RightsComputationException when the evaluation fails, without rendering the "
        + "claims nor raising any alert")
    void testCompute_failedEvaluation() {
        var cause = new OpaEvaluationException("OPA returned no result for the policy of application APP");
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenThrow(cause);

        assertSame(cause, rightsComputationException().getCause());
        verifyNoInteractions(jinjaService);
    }

    @Test
    @DisplayName("compute should throw a RightsComputationException when the claims template cannot be rendered")
    void testCompute_templateError() {
        var cause = new IllegalStateException("Unclosed tag");
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenReturn(new OpaResult(true, List.of()));
        when(jinjaService.render(eq(CLAIMS_TEMPLATE), any())).thenThrow(cause);

        assertSame(cause, rightsComputationException().getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "", "null", "[\"Administrator\"]", "{\"sub\": \"jdoe\"} {}"})
    @DisplayName("compute should throw a RightsComputationException, without raising the unknownRole alert, when the "
        + "claims are not rendered as a JSON object")
    void testCompute_invalidClaims(final String claims) {
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenReturn(new OpaResult(true, List.of("Ghost")));
        when(jinjaService.render(eq(CLAIMS_TEMPLATE), any())).thenReturn(claims);

        assertNotNull(rightsComputationException().getCause());
    }

    @Test
    @DisplayName("compute should propagate the OpaUnreachableException, not wrapped, without raising any alert, "
        + "OpaService having raised the opaUnreachable alert")
    void testCompute_opaUnreachable() {
        var exception = new OpaUnreachableException("Unable to reach the OPA server",
            new ResourceAccessException("Connection refused"));
        when(opaService.evaluate(APPLICATION_CODE, PAYLOAD)).thenThrow(exception);

        assertSame(exception, assertThrows(OpaUnreachableException.class, () -> service.compute(pair(), ROLE_NAMES)));

        verifyNoInteractions(jinjaService);
        assertTrue(alertAppender.list.isEmpty());
    }

    private static Stream<Arguments> validResults() {
        return Stream.of(
            Arguments.of(true, List.of()),
            Arguments.of(false, List.of("Reader")));
    }

    /**
     * Computes the pair, checks that it throws a RightsComputationException naming the pair without raising any alert,
     * and gives the exception.
     */
    private RightsComputationException rightsComputationException() {
        var exception = assertThrows(RightsComputationException.class, () -> service.compute(pair(), ROLE_NAMES));

        assertEquals("Unable to compute the rights of the pair account=jdoe, application=APP", exception.getMessage());
        assertTrue(alertAppender.list.isEmpty());
        return exception;
    }

    private static AccountComputedRightView pair() {
        return new AccountComputedRightView(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe",
            UUID.fromString("00000000-0000-4000-8000-0000000000a1"), APPLICATION_CODE, CLAIMS_TEMPLATE, PAYLOAD,
            "payload-checksum", "script-checksum", "claims-template-checksum", true);
    }
}
