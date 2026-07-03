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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Test class: OpaServiceImpl")
class OpaServiceImplTest {

    private MockRestServiceServer server;

    private OpaServiceImpl service;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://opa:8181");
        this.server = MockRestServiceServer.bindTo(builder).build();
        var template = new ClassPathResource("templates/opa/opa-policy.rego.j2");
        this.service = new OpaServiceImpl(
            new JinjaServiceImpl(), builder.build(), template, "/v1/policies/{id}");
    }

    @Test
    @DisplayName("generate should render the package from the application code and include active fragments")
    void testGenerate() {
        var application = Application.builder().code("payroll").build();
        List<ApplicationRule> rules = List.of(
            ApplicationRule.builder().code("RULE_1").priority(1).script("# marker-fragment").build());

        var result = service.generate(application, rules);

        assertTrue(result.contains("package authz[\"payroll\"]"),
            "The package must be built with brackets from the application code");
        assertTrue(result.contains("# marker-fragment"), "The fragment script must be included");
        assertTrue(result.contains("default final_allow := false"), "The fixed resolution block must be present");
    }

    @Test
    @DisplayName("generate should quote the package so codes containing dashes stay parsable")
    void testGenerate_dashInCode() {
        var application = Application.builder().code("my-app").build();

        var result = service.generate(application, List.of());

        assertTrue(result.contains("package authz[\"my-app\"]"),
            "A code with a dash must be emitted inside brackets, not as a dotted package");
    }

    @Test
    @DisplayName("generate should render fragments as raw scripts without per-rule metadata")
    void testGenerate_rawFragments() {
        var application = Application.builder().code("payroll").build();
        List<ApplicationRule> rules = List.of(
            ApplicationRule.builder().code("RULE_1").priority(1).script("signals contains \"allow\"").build());

        var result = service.generate(application, rules);

        assertTrue(result.contains("signals contains \"allow\""), "The raw fragment script must be present");
        assertFalse(result.contains("--- rule:"), "No per-rule metadata comment must be emitted");
    }

    @Test
    @DisplayName("generate should order fragments by ascending priority regardless of the input order")
    void testGenerate_ordersFragmentsByPriority() {
        var application = Application.builder().code("payroll").build();
        List<ApplicationRule> rules = List.of(
            ApplicationRule.builder().code("RULE_LAST").priority(3).script("# fragment-priority-3").build(),
            ApplicationRule.builder().code("RULE_FIRST").priority(1).script("# fragment-priority-1").build(),
            ApplicationRule.builder().code("RULE_MID").priority(2).script("# fragment-priority-2").build());

        var result = service.generate(application, rules);

        assertTrue(result.indexOf("# fragment-priority-1") < result.indexOf("# fragment-priority-2"),
            "Priority 1 fragment must appear before priority 2");
        assertTrue(result.indexOf("# fragment-priority-2") < result.indexOf("# fragment-priority-3"),
            "Priority 2 fragment must appear before priority 3");
    }

    @Test
    @DisplayName("publish should PUT the policy to the OPA endpoint and return a deployment timestamp")
    void testPublish() {
        var application = Application.builder().code("payroll").script("rendered-policy").build();
        server.expect(requestTo("http://opa:8181/v1/policies/payroll"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string("rendered-policy"))
            .andRespond(withSuccess());

        var deployedAt = service.publish(application);

        assertNotNull(deployedAt);
        server.verify();
    }

    @Test
    @DisplayName("publish should raise an ApiException when the OPA server fails")
    void testPublish_shouldThrowOnServerError() {
        var application = Application.builder().code("payroll").script("rendered-policy").build();
        server.expect(requestTo("http://opa:8181/v1/policies/payroll"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withServerError());

        var exception = assertThrows(ApiException.class, () -> service.publish(application));

        assertEquals(502, exception.getStatusCode());
        assertEquals("error.opa.policy.publish_failed", exception.getError().key());
        server.verify();
    }
}
