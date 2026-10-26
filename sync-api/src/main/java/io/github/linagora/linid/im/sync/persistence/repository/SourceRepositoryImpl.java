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

import io.github.linagora.linid.im.sync.config.LinidDataSourceConfig;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link JdbcTemplate} implementation of {@link SourceRepository}, on the linid-api database.
 */
@Repository
public class SourceRepositoryImpl implements SourceRepository {

    /**
     * Pattern of the accepted source and column names, which cannot hold a quote.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    /**
     * PostgreSQL types of the columns read as JSON.
     */
    private static final Set<String> JSON_TYPES = Set.of("json", "jsonb");

    /**
     * JDBC template of the linid-api database.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Mapper parsing the {@code json} and {@code jsonb} columns.
     */
    private final JsonMapper jsonMapper;

    /**
     * Maps a row to the value of each column by name, converting the PostgreSQL arrays into lists while the result set
     * is open, and parsing the {@code json} and {@code jsonb} columns.
     */
    private final ColumnMapRowMapper rowMapper = new ColumnMapRowMapper() {
        @Override
        protected Object getColumnValue(final ResultSet rs, final int index) throws SQLException {
            Object value = super.getColumnValue(rs, index);
            if (value instanceof Array array) {
                return Arrays.asList((Object[]) array.getArray());
            }

            if (value != null && JSON_TYPES.contains(rs.getMetaData().getColumnTypeName(index))) {
                return jsonMapper.readValue(rs.getString(index), Object.class);
            }
            return value;
        }
    };

    /**
     * Creates the repository on the linid-api database.
     *
     * @param jdbcTemplate the JDBC template of the linid-api database
     * @param jsonMapper   the mapper parsing the {@code json} and {@code jsonb} columns
     */
    public SourceRepositoryImpl(final @Qualifier(LinidDataSourceConfig.QUALIFIER) JdbcTemplate jdbcTemplate,
                                final JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<Map<String, Object>> findAllByAccountIds(
        final String source,
        final String accountIdColumn,
        final Collection<UUID> actIds) {
        String sql = "SELECT * FROM %s WHERE %s = ANY(?)".formatted(quote(source), quote(accountIdColumn));
        return jdbcTemplate.query(sql, rowMapper, (Object) actIds.toArray(UUID[]::new));
    }

    /**
     * Checks a source or column name and quotes it.
     *
     * @param name the name of the source or column
     * @return the quoted name
     * @throws IllegalArgumentException when the name does not match {@code [a-z_][a-z0-9_]*}
     */
    private static String quote(final String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid source or column name: " + name);
        }
        return '"' + name + '"';
    }
}
