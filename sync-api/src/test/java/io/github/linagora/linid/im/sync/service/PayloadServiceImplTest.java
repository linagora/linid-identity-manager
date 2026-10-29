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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: PayloadServiceImpl")
class PayloadServiceImplTest {

    private static final String MAPPING = """
        {
          "external_id": {{ accounts.external_id | tojson }},
          "email": {{ accounts.email | tojson }},
          "organizational_units": {{ account_payload_view.organizational_units | tojson }},
          "groups": {{ account_payload_view.groups | tojson }}
        }
        """;

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private ListAppender<ILoggingEvent> alertAppender;

    @BeforeEach
    void setUp() {
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
    }

    @Test
    @DisplayName("build should render the mapping with the row of the account in each source")
    void testBuild() {
        var payload = service(MAPPING).build(Map.of(
            "accounts", List.of(Map.of("external_id", "jdoe", "email", "jdoe@example.com")),
            "account_payload_view", List.of(Map.of(
                "organizational_units", List.of("Paris", "R&D"), "groups", List.of("DEV", "RND")))));

        assertEquals(Map.of(
            "external_id", "jdoe",
            "email", "jdoe@example.com",
            "organizational_units", List.of("Paris", "R&D"),
            "groups", List.of("DEV", "RND")), payload);
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("build should render a source without row for the account as an empty variable")
    void testBuild_noRow() {
        var payload = service(MAPPING).build(Map.of(
            "accounts", List.of(Map.of("external_id", "jdoe", "email", "jdoe@example.com")),
            "account_payload_view", List.of()));

        var expected = new HashMap<String, Object>();
        expected.put("external_id", "jdoe");
        expected.put("email", "jdoe@example.com");
        expected.put("organizational_units", null);
        expected.put("groups", null);
        assertEquals(expected, payload);
    }

    @Test
    @DisplayName("build should render the mapping without any source")
    void testBuild_noSource() {
        assertEquals(Map.of("application", "linid"), service("{\"application\": \"linid\"}").build(Map.of()));
    }

    @Test
    @DisplayName("build should raise a PayloadException when the account has several rows in a source, without alert")
    void testBuild_severalRows() {
        var service = service("{\"email\": {{ accounts.email | tojson }}}");
        Map<String, List<Map<String, Object>>> rowsBySource = Map.of("accounts", List.of(
            Map.of("email", "jdoe@example.com"), Map.of("email", "john@example.com")));

        var exception = assertThrows(PayloadException.class, () -> service.build(rowsBySource));

        assertEquals("The account has 2 rows in the source accounts", exception.getMessage());
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("build should raise a PayloadException when the data of the account raises a template error")
    void testBuild_templateError() {
        var service = service("{\"birth_date\": {{ accounts.birth_date | tojson }}}");
        Map<String, List<Map<String, Object>>> rowsBySource =
            Map.of("accounts", List.of(Map.of("birth_date", LocalDate.of(1990, 1, 1))));

        var exception = assertThrows(PayloadException.class, () -> service.build(rowsBySource));

        assertTrue(exception.getMessage().contains("Invalid input for 'tojson'"), exception.getMessage());
    }

    @Test
    @DisplayName("build should raise a PayloadException when the data of the account gives an invalid JSON")
    void testBuild_invalidJson() {
        // The separator between the groups is missing: only an account with several groups gives an invalid JSON
        var service = service(
            "{\"groups\": [{% for group in account_payload_view.groups %}{{ group | tojson }}{% endfor %}]}");
        Map<String, List<Map<String, Object>>> rowsBySource =
            Map.of("account_payload_view", List.of(Map.of("groups", List.of("DEV", "RND"))));

        var exception = assertThrows(PayloadException.class, () -> service.build(rowsBySource));

        assertTrue(exception.getMessage().contains("Unexpected character"), exception.getMessage());
        assertEquals(Map.of("groups", List.of("DEV")),
            service.build(Map.of("account_payload_view", List.of(Map.of("groups", List.of("DEV"))))));
    }

    @Test
    @DisplayName("checkMapping should accept a valid mapping, without alert")
    void testCheckMapping() {
        assertDoesNotThrow(() -> service(MAPPING).checkMapping());
        assertTrue(alertAppender.list.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        // Jinjava syntax error
        "{\"email\": {% if accounts.email %}{{ accounts.email | tojson }}}",
        "{\"email\": {{ accounts.email | tojson( }}}",
        // Value inserted without tojson
        "{\"email\": {{ accounts.email }}}",
        // Unknown filter, rendered as an empty value
        "{\"email\": {{ accounts.email | to_json }}}",
        // Unclosed expression, rendered unchanged
        "{\"email\": {{ accounts.email | tojson }}",
        // Not a JSON object
        "[{{ accounts.email | tojson }}]",
        "{{ accounts.email | tojson }}",
        "{\"email\": {{ accounts.email | tojson }}} {}"
    })
    @DisplayName("checkMapping should raise an exception after a blocking alert when the mapping is invalid")
    void testCheckMapping_invalidMapping(final String mapping) {
        var service = service(mapping);

        var exception = assertThrows(IllegalStateException.class, service::checkMapping);

        assertEquals("Invalid template sync-data.job.mapping", exception.getMessage());
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.ERROR, alert.getLevel());
        assertEquals("Invalid mapping template, payload build aborted — property=sync-data.job.mapping",
            alert.getFormattedMessage());
    }

    private static PayloadServiceImpl service(final String mapping) {
        return new PayloadServiceImpl(new SyncDataProperties(Map.of(), mapping), new JinjaServiceImpl(),
            JsonMapper.builder().build());
    }
}
