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

package io.github.linagora.linid.im.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.superset.SupersetTokenDTO;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.service.SupersetService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SupersetController")
class SupersetControllerTest {

    @Mock
    private SupersetService service;

    @InjectMocks
    private SupersetController supersetController;

    private UserPrincipal userPrincipal;

    private static final UUID USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID DASHBOARD_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final String DASHBOARD_SLUG = "user-log-dashboard";

    private static final String GUEST_TOKEN = "guest-token";

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(USER_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("Should create Superset guest token and return 201")
    void testCreate_shouldReturn201WithGuestToken() {
        var tokenRecord = new SupersetTokenRecord(
            DASHBOARD_SLUG,
            DASHBOARD_ID,
            "rls-001"
        );

        var token = SupersetTokenDTO.builder()
            .token(GUEST_TOKEN)
            .build();

        when(service.getToken(userPrincipal, tokenRecord)).thenReturn(token);

        ResponseEntity<SupersetTokenDTO> response =
            supersetController.create(userPrincipal, tokenRecord);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(GUEST_TOKEN, response.getBody().getToken());

        verify(service).getToken(userPrincipal, tokenRecord);
    }

    @Test
    @DisplayName("Should resolve Superset dashboard ID from slug and return 200")
    void testGetDashboardId_shouldReturn200WithDashboardId() {
        when(service.getDashboardId(DASHBOARD_SLUG)).thenReturn(DASHBOARD_ID);

        ResponseEntity<UUID> response =
            supersetController.getDashboardId(userPrincipal, DASHBOARD_SLUG);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(DASHBOARD_ID, response.getBody());

        verify(service).getDashboardId(DASHBOARD_SLUG);
    }
}
