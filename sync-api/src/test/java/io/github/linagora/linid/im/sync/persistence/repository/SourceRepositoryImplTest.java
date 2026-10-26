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

package io.github.linagora.linid.im.sync.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Test class: SourceRepositoryImpl")
class SourceRepositoryImplTest {

    private static final UUID ACCOUNT_1 = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final UUID ACCOUNT_2 = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final SourceRepository repository = new SourceRepositoryImpl(jdbcTemplate, JsonMapper.builder().build());

    @Test
    @DisplayName("findAllByAccountIds should filter the quoted source on the quoted account id column with the act_id")
    @SuppressWarnings("unchecked")
    void testFindAllByAccountIds() {
        List<Map<String, Object>> rows = List.of(Map.of("act_id", ACCOUNT_1));
        when(jdbcTemplate.query(
            eq("SELECT * FROM \"account_payload_view\" WHERE \"owner_id\" = ANY(?)"),
            any(RowMapper.class),
            actIds(ACCOUNT_1, ACCOUNT_2)))
            .thenReturn(rows);

        assertEquals(rows, repository.findAllByAccountIds("account_payload_view", "owner_id",
            List.of(ACCOUNT_1, ACCOUNT_2)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Accounts", "1accounts", "account-view", "public.accounts", "accounts\"; DROP TABLE x; --"})
    @DisplayName("findAllByAccountIds should reject an invalid source name before any query")
    void testFindAllByAccountIds_invalidSource(final String source) {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> repository.findAllByAccountIds(source, "act_id", List.of(ACCOUNT_1)));

        assertEquals("Invalid source or column name: " + source, exception.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Act_id", "9act_id", "act id", "act_id\" OR 1=1 --"})
    @DisplayName("findAllByAccountIds should reject an invalid account id column name before any query")
    void testFindAllByAccountIds_invalidColumn(final String column) {
        assertThrows(IllegalArgumentException.class,
            () -> repository.findAllByAccountIds("accounts", column, List.of(ACCOUNT_1)));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("findAllByAccountIds should accept names made of lowercase letters, digits and underscores")
    void testFindAllByAccountIds_validNames() {
        repository.findAllByAccountIds("_source_2", "account_id_1", List.of(ACCOUNT_1));

        verify(jdbcTemplate).query(eq("SELECT * FROM \"_source_2\" WHERE \"account_id_1\" = ANY(?)"),
            any(RowMapper.class), actIds(ACCOUNT_1));
    }

    @Test
    @DisplayName("findAllByAccountIds should read a PostgreSQL array as a list, and the other values unchanged")
    void testFindAllByAccountIds_array() throws Exception {
        var metaData = mock(ResultSetMetaData.class);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("act_id");
        when(metaData.getColumnTypeName(1)).thenReturn("uuid");
        when(metaData.getColumnLabel(2)).thenReturn("groups");
        when(metaData.getColumnLabel(3)).thenReturn("organizational_units");
        var groups = mock(Array.class);
        when(groups.getArray()).thenReturn(new String[] {"DEV", "RND"});
        var organizationalUnits = mock(Array.class);
        when(organizationalUnits.getArray()).thenReturn(new String[] {"Paris", null});
        var resultSet = mock(ResultSet.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.getObject(1)).thenReturn(ACCOUNT_1);
        when(resultSet.getObject(2)).thenReturn(groups);
        when(resultSet.getObject(3)).thenReturn(organizationalUnits);

        var row = mapRow(resultSet);

        assertEquals(ACCOUNT_1, row.get("act_id"));
        assertEquals(List.of("DEV", "RND"), row.get("groups"));
        assertEquals(Arrays.asList("Paris", null), row.get("organizational_units"));
    }

    @Test
    @DisplayName("findAllByAccountIds should read a json or jsonb value as parsed JSON, and a null value as null")
    void testFindAllByAccountIds_json() throws Exception {
        var metaData = mock(ResultSetMetaData.class);
        when(metaData.getColumnCount()).thenReturn(4);
        when(metaData.getColumnLabel(1)).thenReturn("extra_parameters");
        when(metaData.getColumnTypeName(1)).thenReturn("jsonb");
        when(metaData.getColumnLabel(2)).thenReturn("settings");
        when(metaData.getColumnTypeName(2)).thenReturn("json");
        when(metaData.getColumnLabel(3)).thenReturn("empty_parameters");
        when(metaData.getColumnTypeName(3)).thenReturn("jsonb");
        when(metaData.getColumnLabel(4)).thenReturn("email");
        when(metaData.getColumnTypeName(4)).thenReturn("text");
        var resultSet = mock(ResultSet.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.getObject(1)).thenReturn(new Object());
        when(resultSet.getString(1)).thenReturn("{\"department\": \"R&D\", \"badges\": [1, \"a\"]}");
        when(resultSet.getObject(2)).thenReturn(new Object());
        when(resultSet.getString(2)).thenReturn("\"dark\"");
        when(resultSet.getObject(4)).thenReturn("{\"not\": \"json\"}");

        var row = mapRow(resultSet);

        assertEquals(Map.of("department", "R&D", "badges", List.of(1, "a")), row.get("extra_parameters"));
        assertEquals("dark", row.get("settings"));
        assertNull(row.get("empty_parameters"));
        assertEquals("{\"not\": \"json\"}", row.get("email"));
    }

    /**
     * Matches the array of {@code act_id} bound to the query, a matcher typed as an array being applied by Mockito to
     * the whole varargs instead.
     */
    private static Object actIds(final UUID... actIds) {
        return argThat((UUID[] actual) -> Arrays.equals(actIds, actual));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapRow(final ResultSet resultSet) throws Exception {
        repository.findAllByAccountIds("account_payload_view", "act_id", List.of(ACCOUNT_1));
        var rowMapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbcTemplate).query(any(String.class), rowMapper.capture(), actIds(ACCOUNT_1));
        return (Map<String, Object>) rowMapper.getValue().mapRow(resultSet, 0);
    }
}
