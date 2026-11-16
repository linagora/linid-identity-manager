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

import io.github.linagora.linid.im.sync.config.SchedulingConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link SessionLockService}, opening a new session of the {@code sync-api} database for each lock.
 *
 * <p>The connection comes from the datasource of the session locks, outside the pool. Closing it ends its session, and
 * PostgreSQL releases the lock with it: the lock is never released explicitly, and an instance that crashes while
 * holding a lock does not block the next processings.</p>
 */
@Service
public class SessionLockServiceImpl implements SessionLockService {

    /**
     * Takes a session lock without waiting, giving whether it was taken. PostgreSQL releases it when the session ends.
     */
    static final String TRY_LOCK_SQL = "SELECT pg_try_advisory_lock(?, ?)";

    /**
     * Datasource of the session locks, opening a new session of the {@code sync-api} database at each connection.
     */
    private final DataSource sessionLockDataSource;

    /**
     * Creates the service of the session locks.
     *
     * @param sessionLockDataSource the datasource of the session locks, without pool
     */
    public SessionLockServiceImpl(
        @Qualifier(SchedulingConfig.SESSION_LOCK_QUALIFIER) final DataSource sessionLockDataSource) {
        this.sessionLockDataSource = sessionLockDataSource;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the lock is already held, or when it cannot be requested, the connection is closed at once.</p>
     */
    @Override
    public Optional<SessionLock> tryLock(final int namespace, final int key) {
        Connection connection = openConnection();
        SessionLock lock = () -> JdbcUtils.closeConnection(connection);
        try {
            if (Boolean.TRUE.equals(new JdbcTemplate(new SingleConnectionDataSource(connection, true))
                .queryForObject(TRY_LOCK_SQL, Boolean.class, namespace, key))) {
                return Optional.of(lock);
            }
        } catch (RuntimeException e) {
            lock.close();
            throw e;
        }
        lock.close();
        return Optional.empty();
    }

    /**
     * Opens a new session of the {@code sync-api} database.
     *
     * @return the dedicated connection
     * @throws CannotGetJdbcConnectionException when the {@code sync-api} database cannot be reached
     */
    private Connection openConnection() {
        try {
            return sessionLockDataSource.getConnection();
        } catch (SQLException e) {
            throw new CannotGetJdbcConnectionException("The sync-api database cannot be reached", e);
        }
    }
}
