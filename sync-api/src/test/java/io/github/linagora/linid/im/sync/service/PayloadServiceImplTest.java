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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.config.SourceProperties;
import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.model.payload.PayloadItem;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: PayloadServiceImpl")
class PayloadServiceImplTest {

    private static final Map<String, SourceProperties> SOURCES = Map.of(
        "accounts", new SourceProperties("act_id"),
        "account_payload_view", new SourceProperties("act_id"));

    private static final String MAPPING = """
        {
          "external_id": {{ accounts.external_id | tojson }},
          "email": {{ accounts.email | tojson }},
          "organizational_units": {{ account_payload_view.organizational_units | tojson }},
          "groups": {{ account_payload_view.groups | tojson }}
        }
        """;

    private static final AccountIdentity JDOE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe");

    private static final AccountIdentity ASMITH =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000002"), "asmith");

    private static final AccountIdentity BLEE =
        new AccountIdentity(UUID.fromString("00000000-0000-4000-8000-000000000003"), "blee");

    private final SourceRepository sourceRepository = mock(SourceRepository.class);

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
    @DisplayName("build should read each source once, and render the mapping with the row of each account in it")
    void testBuild() {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", actIds(JDOE, ASMITH))).thenReturn(List.of(
            Map.of("act_id", JDOE.actId(), "external_id", "jdoe", "email", "jdoe@example.com"),
            Map.of("act_id", ASMITH.actId(), "external_id", "asmith", "email", "asmith@example.com")));
        when(sourceRepository.findAllByAccountIds("account_payload_view", "act_id", actIds(JDOE, ASMITH)))
            .thenReturn(List.of(
                Map.of("act_id", JDOE.actId(), "organizational_units", List.of("Paris", "R&D"),
                    "groups", List.of("DEV", "RND")),
                Map.of("act_id", ASMITH.actId(), "organizational_units", List.of(), "groups", List.of())));

        var items = service(MAPPING).build(List.of(JDOE, ASMITH));

        assertEquals(List.of(
            new PayloadItem(JDOE.actId(), "jdoe", Map.of(
                "external_id", "jdoe",
                "email", "jdoe@example.com",
                "organizational_units", List.of("Paris", "R&D"),
                "groups", List.of("DEV", "RND"))),
            new PayloadItem(ASMITH.actId(), "asmith", Map.of(
                "external_id", "asmith",
                "email", "asmith@example.com",
                "organizational_units", List.of(),
                "groups", List.of()))), items);
        verify(sourceRepository).findAllByAccountIds("accounts", "act_id", actIds(JDOE, ASMITH));
        verify(sourceRepository).findAllByAccountIds("account_payload_view", "act_id", actIds(JDOE, ASMITH));
        verifyNoMoreInteractions(sourceRepository);
        assertTrue(alertAppender.list.isEmpty());
    }

    @Test
    @DisplayName("build should give each account the rows of a source holding its act_id in the account id column")
    void testBuild_accountIdColumn() {
        when(sourceRepository.findAllByAccountIds("phones", "owner_id", actIds(JDOE, ASMITH))).thenReturn(List.of(
            Map.of("owner_id", ASMITH.actId(), "number", "0607080910"),
            Map.of("owner_id", JDOE.actId(), "number", "0102030405")));
        var service = service(Map.of("phones", new SourceProperties("owner_id")),
            "{\"phone\": {{ phones.number | tojson }}}");

        var items = service.build(List.of(JDOE, ASMITH));

        assertEquals(List.of(
            new PayloadItem(JDOE.actId(), "jdoe", Map.of("phone", "0102030405")),
            new PayloadItem(ASMITH.actId(), "asmith", Map.of("phone", "0607080910"))), items);
    }

    @Test
    @DisplayName("build should filter a source on act_id when its account id column is not set")
    void testBuild_defaultAccountIdColumn() {
        service(Map.of("accounts", new SourceProperties(null)), "{}").build(List.of(JDOE));

        verify(sourceRepository).findAllByAccountIds("accounts", "act_id", actIds(JDOE));
    }

    @Test
    @DisplayName("build should render a source without row for an account as an empty variable")
    void testBuild_noRow() {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", actIds(JDOE))).thenReturn(List.of(
            Map.of("act_id", JDOE.actId(), "external_id", "jdoe", "email", "jdoe@example.com")));

        var items = service(MAPPING).build(List.of(JDOE));

        var payload = new HashMap<String, Object>();
        payload.put("external_id", "jdoe");
        payload.put("email", "jdoe@example.com");
        payload.put("organizational_units", null);
        payload.put("groups", null);
        assertEquals(List.of(new PayloadItem(JDOE.actId(), "jdoe", payload)), items);
    }

    @Test
    @DisplayName("build should render the mapping without any source")
    void testBuild_noSource() {
        var items = service(Map.of(), "{\"application\": \"linid\"}").build(List.of(JDOE));

        assertEquals(List.of(new PayloadItem(JDOE.actId(), "jdoe", Map.of("application", "linid"))), items);
        verifyNoInteractions(sourceRepository);
    }

    @Test
    @DisplayName("build should leave out an account with several rows in a source, and build the other accounts")
    void testBuild_severalRows() {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", actIds(JDOE, ASMITH))).thenReturn(List.of(
            Map.of("act_id", JDOE.actId(), "email", "jdoe@example.com"),
            Map.of("act_id", JDOE.actId(), "email", "john@example.com"),
            Map.of("act_id", ASMITH.actId(), "email", "asmith@example.com")));

        var items = service("{\"email\": {{ accounts.email | tojson }}}").build(List.of(JDOE, ASMITH));

        assertEquals(List.of(new PayloadItem(ASMITH.actId(), "asmith", Map.of("email", "asmith@example.com"))), items);
        assertPayloadError("account=jdoe", "The account has 2 rows in the source accounts");
    }

    @Test
    @DisplayName("build should leave out an account whose data raises a template error, and build the other accounts")
    void testBuild_templateError() {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", actIds(JDOE, ASMITH))).thenReturn(List.of(
            Map.of("act_id", JDOE.actId(), "birth_date", LocalDate.of(1990, 1, 1)),
            Map.of("act_id", ASMITH.actId(), "birth_date", "1990-01-01")));

        var items = service("{\"birth_date\": {{ accounts.birth_date | tojson }}}").build(List.of(JDOE, ASMITH));

        assertEquals(List.of(new PayloadItem(ASMITH.actId(), "asmith", Map.of("birth_date", "1990-01-01"))), items);
        assertPayloadError("account=jdoe", "Invalid input for 'tojson'");
    }

    @Test
    @DisplayName("build should leave out an account whose data gives an invalid JSON, and build the other accounts")
    void testBuild_invalidJson() {
        when(sourceRepository.findAllByAccountIds("account_payload_view", "act_id", actIds(JDOE, ASMITH, BLEE)))
            .thenReturn(List.of(
                Map.of("act_id", JDOE.actId(), "groups", List.of("DEV", "RND")),
                Map.of("act_id", ASMITH.actId(), "groups", List.of("DEV")),
                Map.of("act_id", BLEE.actId(), "groups", List.of())));
        // The separator between the groups is missing: only an account with several groups gives an invalid JSON
        var mapping = "{\"groups\": [{% for group in account_payload_view.groups %}{{ group | tojson }}{% endfor %}]}";

        var items = service(mapping).build(List.of(JDOE, ASMITH, BLEE));

        assertEquals(List.of(
            new PayloadItem(ASMITH.actId(), "asmith", Map.of("groups", List.of("DEV"))),
            new PayloadItem(BLEE.actId(), "blee", Map.of("groups", List.of()))), items);
        assertPayloadError("account=jdoe", "Unexpected character");
    }

    @Test
    @DisplayName("build should return no payload when every account is in error")
    void testBuild_everyAccountInError() {
        when(sourceRepository.findAllByAccountIds("accounts", "act_id", actIds(JDOE))).thenReturn(List.of(
            Map.of("act_id", JDOE.actId()), Map.of("act_id", JDOE.actId())));

        assertEquals(List.of(), service(MAPPING).build(List.of(JDOE)));
        assertEquals(1, alertAppender.list.size());
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
    @DisplayName("build should raise an exception before reading any source when the mapping is invalid")
    void testBuild_invalidMapping(final String mapping) {
        var service = service(mapping);
        var accounts = List.of(JDOE);

        var exception = assertThrows(IllegalStateException.class, () -> service.build(accounts));

        assertEquals("Invalid template sync-data.job.mapping", exception.getMessage());
        verifyNoInteractions(sourceRepository);
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.ERROR, alert.getLevel());
        assertEquals("Invalid mapping template, payload build aborted — property=sync-data.job.mapping",
            alert.getFormattedMessage());
    }

    @Test
    @DisplayName("build should raise an exception naming the source that cannot be read, after a blocking alert")
    void testBuild_unreadableSource() {
        var cause = new BadSqlGrammarException("read", "SELECT", new SQLException("relation does not exist"));
        when(sourceRepository.findAllByAccountIds(eq("unknown_view"), any(), anyCollection())).thenThrow(cause);
        var sources = new LinkedHashMap<String, SourceProperties>();
        sources.put("accounts", new SourceProperties("act_id"));
        sources.put("unknown_view", new SourceProperties("act_id"));
        var service = service(sources, MAPPING);
        var accounts = List.of(JDOE);

        var exception = assertThrows(IllegalStateException.class, () -> service.build(accounts));

        assertEquals("Unable to read the source unknown_view", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.ERROR, alert.getLevel());
        assertEquals("Source unreadable, payload build aborted — source=unknown_view", alert.getFormattedMessage());
    }

    @Test
    @DisplayName("build should raise an exception naming the source whose name is invalid")
    void testBuild_invalidSourceName() {
        var cause = new IllegalArgumentException("Invalid source or column name: Accounts");
        when(sourceRepository.findAllByAccountIds("Accounts", "act_id", actIds(JDOE))).thenThrow(cause);
        var service = service(Map.of("Accounts", new SourceProperties("act_id")), "{}");
        var accounts = List.of(JDOE);

        var exception = assertThrows(IllegalStateException.class, () -> service.build(accounts));

        assertEquals("Unable to read the source Accounts", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Source unreadable, payload build aborted — source=Accounts",
            alertAppender.list.getFirst().getFormattedMessage());
    }

    private PayloadService service(final String mapping) {
        return service(SOURCES, mapping);
    }

    private PayloadService service(final Map<String, SourceProperties> sources, final String mapping) {
        return new PayloadServiceImpl(new SyncDataProperties(sources, mapping), sourceRepository,
            new JinjaServiceImpl(), JsonMapper.builder().build());
    }

    private static List<UUID> actIds(final AccountIdentity... accounts) {
        return Stream.of(accounts).map(AccountIdentity::actId).toList();
    }

    private void assertPayloadError(final String context, final String causeMessage) {
        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.WARN, alert.getLevel());
        assertEquals("Account in error during payload build, keeping previous payload — " + context,
            alert.getFormattedMessage());
        assertTrue(alert.getThrowableProxy().getMessage().contains(causeMessage),
            alert.getThrowableProxy().getMessage());
    }
}
