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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AccountLockServiceImpl")
class AccountLockServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AccountLockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountLockServiceImpl(jdbcTemplate, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("lockAccounts should limit the wait for the locks, then take one lock per distinct key, in the order "
        + "of the keys, with the namespace of the per-account lock")
    void testLockAccounts() {
        // The hash code of a UUID folds its 128 bits: (0, 5) and (5, 0) share the key 5
        var key5 = new UUID(0L, 5L);
        var sameKey5 = new UUID(5L, 0L);
        var key1 = new UUID(0L, 1L);
        var key3 = new UUID(0L, 3L);
        var minKey = new UUID(0L, 0x8000_0000L);

        service.lockAccounts(List.of(key5, key1, sameKey5, key1, minKey, key3));

        var order = inOrder(jdbcTemplate);
        order.verify(jdbcTemplate).query(eq(AccountLockServiceImpl.LOCK_TIMEOUT_SQL), any(RowCallbackHandler.class),
            eq("1800000"));
        order.verify(jdbcTemplate).query(eq(AccountLockServiceImpl.LOCK_SQL), any(RowCallbackHandler.class),
            eq(LockNamespaces.ACCOUNT), aryEq(new Integer[] {Integer.MIN_VALUE, 1, 3, 5}));
    }

    @Test
    @DisplayName("lockAccounts should not query the database without account")
    void testLockAccounts_noAccount() {
        service.lockAccounts(List.of());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("constructor should reject a maximum wait for the locks that is not positive")
    void testConstructor_lockTimeoutNotPositive() {
        var exception = assertThrows(IllegalArgumentException.class,
            () -> new AccountLockServiceImpl(jdbcTemplate, Duration.ZERO));

        assertEquals("sync.lock-timeout must be positive", exception.getMessage());
    }
}
