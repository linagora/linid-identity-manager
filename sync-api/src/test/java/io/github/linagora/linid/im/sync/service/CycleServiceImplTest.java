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
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.model.cycle.CycleStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: CycleServiceImpl")
class CycleServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CycleServiceImpl(jdbcTemplate);
    }

    @Test
    @DisplayName("getCycleStatus should answer running while the cycle lock is held")
    void testGetCycleStatus_running() {
        when(jdbcTemplate.queryForObject(CycleServiceImpl.CYCLE_RUNNING_SQL, Boolean.class, LockNamespaces.CYCLE,
            LockNamespaces.CYCLE_KEY)).thenReturn(true);

        assertEquals(new CycleStatusDTO(true), service.getCycleStatus());
    }

    @Test
    @DisplayName("getCycleStatus should answer not running while the cycle lock is free")
    void testGetCycleStatus_notRunning() {
        when(jdbcTemplate.queryForObject(CycleServiceImpl.CYCLE_RUNNING_SQL, Boolean.class, LockNamespaces.CYCLE,
            LockNamespaces.CYCLE_KEY)).thenReturn(false);

        assertEquals(new CycleStatusDTO(false), service.getCycleStatus());
    }
}
