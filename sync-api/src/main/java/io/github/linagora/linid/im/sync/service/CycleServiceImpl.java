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

import io.github.linagora.linid.im.sync.model.cycle.CycleStatusDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link CycleService}, reading the cycle lock of the {@code sync-api} database.
 *
 * <p>The cycle is running while the session lock {@code (LockNamespaces.CYCLE, LockNamespaces.CYCLE_KEY)}, taken by
 * {@link SyncScheduler} during the whole cycle, is held, by any instance. Unlike an execution of the Spring Batch
 * tables, which stays {@code STARTED} after a crash, the lock disappears with the instance that held it.</p>
 */
@Service
public class CycleServiceImpl implements CycleService {

    /**
     * Tells whether the cycle lock {@code (namespace, key)} is held in the current database, by any session: the
     * two-key form of an advisory lock is listed in {@code pg_locks} with the namespace as {@code classid}, the key as
     * {@code objid} and {@code objsubid = 2}. {@code pg_locks} lists the locks of every database of the server.
     */
    static final String CYCLE_RUNNING_SQL = "SELECT EXISTS (SELECT 1 FROM pg_locks WHERE locktype = 'advisory'"
        + " AND database = (SELECT oid FROM pg_database WHERE datname = current_database())"
        + " AND classid = CAST(? AS oid) AND objid = CAST(? AS oid) AND objsubid = 2)";

    /**
     * JDBC template of the {@code sync-api} database, reading the locks held.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the service of the status of the cycle.
     *
     * @param jdbcTemplate the JDBC template of the {@code sync-api} database
     */
    public CycleServiceImpl(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CycleStatusDTO getCycleStatus() {
        return new CycleStatusDTO(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            CYCLE_RUNNING_SQL, Boolean.class, LockNamespaces.CYCLE, LockNamespaces.CYCLE_KEY)));
    }
}
