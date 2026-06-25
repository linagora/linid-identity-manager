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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceDTO;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceRecord;
import io.github.linagora.linid.im.api.persistence.model.UserPreference;
import io.github.linagora.linid.im.api.service.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: UserPreferenceController")
class UserPreferenceControllerTest {

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    private UserPrincipal userPrincipal;

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    private UserPreference createSampleEntity(final String key, final String value) {
        UserPreference entity = new UserPreference();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userPrincipal.getId());
        entity.setKey(key);
        entity.setValue(value);
        entity.setCreatedBy(ADMIN_ID);
        entity.setUpdatedBy(ADMIN_ID);
        return entity;
    }

    private UserPreferenceDTO createSampleDTO(final String key, final String value) {
        UserPreferenceDTO dto = new UserPreferenceDTO();
        dto.setKey(key);
        dto.setValue(value);
        return dto;
    }

    @Test
    @DisplayName("Should save user preference and return 201")
    void testSave_shouldReturn201WithUserPreferenceDTO() {

        var request = new UserPreferenceRecord("theme", "dark");
        var entity = createSampleEntity("theme", "dark");
        var dto = createSampleDTO("theme", "dark");
        when(userPreferenceService.save(userPrincipal, request)).thenReturn(entity);
        when(userPreferenceMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<UserPreferenceDTO> response = userPreferenceController.save(userPrincipal, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("theme", response.getBody().getKey());
        assertEquals("dark", response.getBody().getValue());
        verify(userPreferenceService).save(userPrincipal, request);
        verify(userPreferenceMapper).toDTO(entity);
    }

    @Test
    @DisplayName("Should delete user preference and return 204")
    void testDelete_shouldReturn204() {
        String key = "theme";
        ResponseEntity<Void> response = userPreferenceController.delete(userPrincipal, key);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userPreferenceService).delete(userPrincipal, key);
    }

    @Test
    @DisplayName("Should return user preferences aggregated as a map with 200")
    void testFindAll_shouldReturn200withMap() {
        var first = createSampleEntity("theme", "dark");
        var second = createSampleEntity("language", "fr");
        when(userPreferenceService.findAll(userPrincipal)).thenReturn(List.of(first, second));

        ResponseEntity<Map<String, String>> response = userPreferenceController.findAll(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("fr", response.getBody().get("language"));
        assertEquals("dark", response.getBody().get("theme"));
        verify(userPreferenceService).findAll(userPrincipal);
    }

    @Test
    @DisplayName("Should return an empty map when the user has no preference")
    void testFindAll_shouldReturnEmptyMapWhenNone() {
        when(userPreferenceService.findAll(userPrincipal)).thenReturn(List.of());
        ResponseEntity<Map<String, String>> response = userPreferenceController.findAll(userPrincipal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(userPreferenceService).findAll(userPrincipal);
    }

}
