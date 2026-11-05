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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SessionLockServiceImpl")
class SessionLockServiceImplTest {

    private static final int NAMESPACE = 3;

    private static final int KEY = 42;

    @Mock
    private DataSource sessionLockDataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement tryLockStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData resultSetMetaData;

    private SessionLockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionLockServiceImpl(sessionLockDataSource);
    }

    @Test
    @DisplayName("tryLock should take the lock on a dedicated connection, kept open until the lock is closed")
    void testTryLock() throws Exception {
        lockQuery(true);

        var lock = service.tryLock(NAMESPACE, KEY);

        assertTrue(lock.isPresent());
        verify(tryLockStatement).setObject(1, NAMESPACE);
        verify(tryLockStatement).setObject(2, KEY);
        verify(tryLockStatement).close();
        verify(connection, never()).close();

        lock.get().close();

        verify(connection).close();
    }

    @Test
    @DisplayName("tryLock should give no lock and close the connection when the lock is already held")
    void testTryLock_alreadyHeld() throws Exception {
        lockQuery(false);

        assertTrue(service.tryLock(NAMESPACE, KEY).isEmpty());

        verify(connection).close();
    }

    @Test
    @DisplayName("tryLock should close the connection and let the exception through when the lock cannot be requested")
    void testTryLock_lockError() throws Exception {
        when(sessionLockDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SessionLockServiceImpl.TRY_LOCK_SQL))
            .thenThrow(new SQLException("statement timeout"));

        assertThrows(DataAccessException.class, () -> service.tryLock(NAMESPACE, KEY));

        verify(connection).close();
    }

    @Test
    @DisplayName("tryLock should raise CannotGetJdbcConnectionException when the sync-api database cannot be reached")
    void testTryLock_noConnection() throws Exception {
        when(sessionLockDataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        assertThrows(CannotGetJdbcConnectionException.class, () -> service.tryLock(NAMESPACE, KEY));
    }

    @Test
    @DisplayName("close should not let an error of the connection through")
    void testClose_closeError() throws Exception {
        lockQuery(true);
        doThrow(new SQLException("connection reset")).when(connection).close();
        var lock = service.tryLock(NAMESPACE, KEY).orElseThrow();

        assertDoesNotThrow(lock::close);

        verify(connection).close();
    }

    private void lockQuery(final boolean taken) throws SQLException {
        when(sessionLockDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SessionLockServiceImpl.TRY_LOCK_SQL)).thenReturn(tryLockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSet.getBoolean(1)).thenReturn(taken);
    }
}
