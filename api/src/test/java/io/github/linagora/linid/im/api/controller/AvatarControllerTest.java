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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.service.AccountService;
import io.github.linagora.linid.im.api.service.ApplicationService;
import io.github.linagora.linid.im.api.service.AvatarService;
import io.github.linagora.linid.im.api.service.GroupService;
import io.github.linagora.linid.im.api.service.OrganizationalUnitService;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AvatarController")
class AvatarControllerTest {

    @Mock
    private AvatarService avatarService;

    @Mock
    private AccountService accountService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private OrganizationalUnitService organizationalUnitService;

    @Mock
    private GroupService groupService;

    private AvatarController controller;

    private UserPrincipal userPrincipal;

    private UUID id;

    @BeforeEach
    void setUp() {
        controller = new AvatarController(avatarService, accountService, applicationService,
            organizationalUnitService, groupService);
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should upload the avatar and return 204")
    void testUpload() {
        var file = new MockMultipartFile("file", "me.png", "image/png", new byte[] {1});

        var response = controller.upload(userPrincipal, "accounts", id, file);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(accountService).existsById(userPrincipal, id);
        verify(avatarService).upload("accounts", id, file);
    }

    @Test
    @DisplayName("Should delete the avatar and return 204")
    void testDelete() {
        var response = controller.delete(userPrincipal, "applications", id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(applicationService).existsById(userPrincipal, id);
        verify(avatarService).delete("applications", id);
    }

    @Test
    @DisplayName("Should check the organizational unit before deleting its avatar")
    void testDeleteOrganizationalUnit() {
        controller.delete(userPrincipal, "organizational-units", id);

        verify(organizationalUnitService).existsById(userPrincipal, id);
        verify(avatarService).delete("organizational-units", id);
    }

    @Test
    @DisplayName("Should check the group before deleting its avatar")
    void testDeleteGroup() {
        controller.delete(userPrincipal, "groups", id);

        verify(groupService).existsById(userPrincipal, id);
        verify(avatarService).delete("groups", id);
    }

    @Test
    @DisplayName("Should return 400 when the entity type is not supported")
    void testUnsupportedEntity() {
        var exception = assertThrows(ApiException.class, () -> controller.delete(userPrincipal, "roles", id));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.entity.unsupported", exception.getError().key());
        assertEquals(Map.of("entity", "roles"), exception.getError().context());
        verifyNoInteractions(avatarService);
    }

    @Test
    @DisplayName("Should propagate the not found error of the entity service")
    void testUnknownEntity() {
        doThrow(new ApiException(404, I18nMessage.of("error.account.not_found", Map.of("id", id.toString()))))
            .when(accountService).existsById(userPrincipal, id);

        var exception = assertThrows(ApiException.class, () -> controller.delete(userPrincipal, "accounts", id));

        assertEquals(404, exception.getStatusCode());
        verifyNoInteractions(avatarService);
    }
}
