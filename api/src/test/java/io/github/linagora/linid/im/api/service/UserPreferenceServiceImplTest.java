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

package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceRecord;
import io.github.linagora.linid.im.api.persistence.model.UserPreference;
import io.github.linagora.linid.im.api.persistence.repository.UserPreferenceRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: UserPreferenceServiceImpl")
class UserPreferenceServiceImplTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @InjectMocks
    private UserPreferenceServiceImpl userPreferenceServiceImpl;

    private UserPrincipal userPrincipal;

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("test@example.com");
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

    @Test
    @DisplayName("save should update the value of an existing preference without creating a new one")
    void testSave_shouldUpdateExistingPreference() {
        var request = new UserPreferenceRecord("theme", "light");
        var existing = createSampleEntity("theme", "dark");
        when(userPreferenceRepository.findByUserIdAndKey(userPrincipal.getId(), "theme"))
                .thenReturn(Optional.of(existing));
        when(userPreferenceRepository.save(existing)).thenReturn(existing);

        UserPreference result = userPreferenceServiceImpl.save(userPrincipal, request);


        assertSame(existing, result);
        assertEquals("light", result.getValue());
        verify(userPreferenceRepository).save(existing);
        verify(userPreferenceMapper, never()).toUserPreference(any(), any());
    }

    @Test
    @DisplayName("save should create a new preference when none exists for the key")
    void testSave_shouldCreateWhenNotExisting() {
        var request = new UserPreferenceRecord("theme", "dark");
        var mapped = createSampleEntity("theme", "dark");
        when(userPreferenceRepository.findByUserIdAndKey(userPrincipal.getId(), "theme"))
        .thenReturn(Optional.empty());
        when(userPreferenceMapper.toUserPreference(request, userPrincipal)).thenReturn(mapped);
        when(userPreferenceRepository.save(mapped)).thenReturn(mapped);

        UserPreference result = userPreferenceServiceImpl.save(userPrincipal, request);

        assertSame(mapped, result);
        verify(userPreferenceMapper).toUserPreference(request, userPrincipal);
        verify(userPreferenceRepository).save(mapped);
    }

    @Test
    @DisplayName("delete should remove the preference when it exists")
    void testDelete_shouldDeleteWhenFound() {
        var existing = createSampleEntity("theme", "dark");
        when(userPreferenceRepository.findByUserIdAndKey(userPrincipal.getId(), "theme"))
        .thenReturn(Optional.of(existing));

        userPreferenceServiceImpl.delete(userPrincipal, "theme");

        verify(userPreferenceRepository).delete(existing);
    }

    @Test
    @DisplayName("delete should throw ApiException 404 when the preference does not exist")
    void testDelete_shouldThrow404WhenNotFound() {
        when(userPreferenceRepository.findByUserIdAndKey(userPrincipal.getId(), "theme"))
        .thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> userPreferenceServiceImpl.delete(userPrincipal, "theme"));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.user_preference.not_found", exception.getError().key());
        verify(userPreferenceRepository, never()).delete(any(UserPreference.class));
    }

    @Test
    @DisplayName("findAll should return the preferences of the authenticated user")
    void testFindAll_shouldReturnAllPreferences() {
        var first = createSampleEntity("theme", "dark");
        var second = createSampleEntity("language", "fr");
        when(userPreferenceRepository.findByUserId(userPrincipal.getId()))
                .thenReturn(List.of(first, second));

        List<UserPreference> result = userPreferenceServiceImpl.findAll(userPrincipal);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userPreferenceRepository).findByUserId(userPrincipal.getId());
    }

    @Test
    @DisplayName("findAll should return an empty list when the user has no preference")
    void testFindAll_shouldReturnEmptyListWhenNone() {
        when(userPreferenceRepository.findByUserId(userPrincipal.getId()))
                .thenReturn(List.of());

        List<UserPreference> result = userPreferenceServiceImpl.findAll(userPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userPreferenceRepository).findByUserId(userPrincipal.getId());
    }
}
