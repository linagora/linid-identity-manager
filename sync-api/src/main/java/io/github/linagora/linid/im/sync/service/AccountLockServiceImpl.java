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

import io.github.linagora.linid.im.sync.config.LinidDataSourceConfig;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL implementation of {@link AccountLockService}, with transaction-level advisory locks of the linid-api
 * database.
 *
 * <p>The lock of an account is {@code (LockNamespaces.ACCOUNT, key)}, the key being the hash code of its
 * {@code act_id}: a 128-bit UUID is too large for a lock key. Two accounts with the same key only wait for each
 * other. The locks are taken once per key, in the order of the keys, so that two processings never take the same keys
 * in different orders and deadlock. The keys are not ordered by {@code act_id}: two accounts sharing a key could then
 * take it in two different orders.</p>
 *
 * <p>The locks are taken with the {@link JdbcTemplate} of the linid-api database, in native SQL since Spring Data has
 * no method for the advisory locks: they belong to the transaction of the chunk that writes the accounts, and are
 * held until its commit.</p>
 *
 * <p>The wait for the locks is limited by {@code sync.lock-timeout}: a transaction left open by a frozen or
 * disconnected instance, holding locks of the chunk, makes the chunk fail instead of blocking it without end.</p>
 */
@Service
public class AccountLockServiceImpl implements AccountLockService {

    /**
     * Takes the lock of each key of the array, in the order of the array, whose elements {@code unnest} reads in
     * storage order. The rows carry no value: the locks are taken while PostgreSQL computes them.
     */
    static final String LOCK_SQL = "SELECT pg_advisory_xact_lock(?, lock_key) FROM unnest(?::int[]) AS lock_key";

    /**
     * Sets the {@code lock_timeout} of PostgreSQL, in milliseconds, until the end of the transaction: a wait for a
     * lock beyond it makes the statement fail.
     */
    static final String LOCK_TIMEOUT_SQL = "SELECT set_config('lock_timeout', ?, true)";

    /**
     * JDBC template of the linid-api database.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Maximum wait for the locks, in milliseconds, as the value of {@code lock_timeout}.
     */
    private final String lockTimeout;

    /**
     * Creates the service on the linid-api database.
     *
     * @param jdbcTemplate the JDBC template of the linid-api database
     * @param lockTimeout  the maximum wait for the locks of a chunk
     * @throws IllegalArgumentException when the maximum wait is not positive
     */
    public AccountLockServiceImpl(final @Qualifier(LinidDataSourceConfig.QUALIFIER) JdbcTemplate jdbcTemplate,
                                  @Value("${sync.lock-timeout}") final Duration lockTimeout) {
        Validate.isTrue(lockTimeout.isPositive(), "sync.lock-timeout must be positive");
        this.jdbcTemplate = jdbcTemplate;
        this.lockTimeout = String.valueOf(lockTimeout.toMillis());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Requires a transaction of the linid-api database: outside of it, each statement would commit at once, and the
     * locks would be released before the writes they protect.</p>
     *
     * @throws org.springframework.transaction.IllegalTransactionStateException when no transaction of the linid-api
     *     database is active
     * @throws org.springframework.dao.DataAccessException when a lock is not taken within {@code sync.lock-timeout}
     */
    @Override
    @Transactional(transactionManager = LinidDataSourceConfig.QUALIFIER, propagation = Propagation.MANDATORY)
    public void lockAccounts(final Collection<UUID> accountIds) {
        Integer[] keys = accountIds.stream().map(UUID::hashCode).distinct().sorted().toArray(Integer[]::new);
        if (keys.length == 0) {
            return;
        }
        jdbcTemplate.query(LOCK_TIMEOUT_SQL, resultSet -> { }, lockTimeout);
        jdbcTemplate.query(LOCK_SQL, resultSet -> { }, LockNamespaces.ACCOUNT, keys);
    }
}
