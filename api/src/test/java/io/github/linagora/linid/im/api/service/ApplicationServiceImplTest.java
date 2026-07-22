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

import io.github.linagora.linid.im.api.model.application.ApplicationMapper;
import io.github.linagora.linid.im.api.model.application.ApplicationRecord;
import io.github.linagora.linid.im.api.model.application.ApplicationRoleDTO;
import io.github.linagora.linid.im.api.model.application.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.api.persistence.model.ApplicationView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ApplicationServiceImpl")
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationViewRepository applicationViewRepository;

    @Mock
    private ApplicationMapper mapper;

    @Mock
    private ApplicationRuleRepository applicationRuleRepository;

    @Mock
    private OpaService opaService;

    @Mock
    private ChecksumService checksumService;

    @InjectMocks
    private ApplicationServiceImpl service;

    private UserPrincipal userPrincipal;

    private ApplicationRecord record;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        record = new ApplicationRecord("my-app", "My Application", "desc", "OIDC", "{}");
    }

    @Test
    @DisplayName("create should persist the mapped application")
    void testCreate() {
        var mapped = Application.builder().code("my-app").build();
        when(applicationRepository.findByCode("my-app")).thenReturn(Optional.empty());
        when(mapper.toEntity(record, userPrincipal)).thenReturn(mapped);
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, record);

        var captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());

        // The mapped entity must be the one persisted, unchanged.
        assertSame(mapped, captor.getValue());
        assertSame(mapped, result);
    }

    @Test
    @DisplayName("create should throw when the code already exists")
    void testCreate_shouldThrowOnDuplicateCode() {
        when(applicationRepository.findByCode("my-app")).thenReturn(Optional.of(new Application()));

        var exception = assertThrows(ApiException.class, () -> service.create(userPrincipal, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application.code.already_exists", exception.getError().key());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById should throw when the application does not exist")
    void testFindById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.findById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findViewById should throw when the application does not exist")
    void testFindViewById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationViewRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.findViewById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findAll should query the view repository with a specification")
    void testFindAll() {
        when(applicationViewRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(new ApplicationView())));

        var page = service.findAll(userPrincipal, new ApplicationViewQueryFilterDto(), Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("update should apply record fields, leave externally-managed fields untouched and persist the same entity")
    void testUpdate() {
        var id = UUID.randomUUID();
        var existing = Application.builder()
            .id(id)
            .code("old-code")
            .script("existing-script")
            .scriptChecksum("existing-checksum")
            .deployedAt(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
            .configuration("existing-config")
            .build();
        when(applicationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, id, record);

        var captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        var saved = captor.getValue();

        // The persisted entity must be the loaded one, with every record field propagated.
        assertSame(existing, saved);
        assertSame(saved, result);
        assertEquals(id, saved.getId());
        assertEquals("my-app", saved.getCode());
        assertEquals("My Application", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals("OIDC", saved.getType());
        assertEquals("{}", saved.getClaimsTemplate());
        assertEquals(userPrincipal.getId(), saved.getUpdatedBy());
        // The script, checksum, deployedAt and configuration are managed by a separate process
        // and must be left untouched.
        assertEquals("existing-script", saved.getScript());
        assertEquals("existing-checksum", saved.getScriptChecksum());
        assertEquals(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), saved.getDeployedAt());
        assertEquals("existing-config", saved.getConfiguration());
    }

    @Test
    @DisplayName("update should throw when another application already uses the code")
    void testUpdate_shouldThrowOnDuplicateCode() {
        var id = UUID.randomUUID();
        var existing = Application.builder().id(id).code("old-code").build();
        when(applicationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(applicationRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(new Application()));

        var exception = assertThrows(ApiException.class, () -> service.update(userPrincipal, id, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application.code.already_exists", exception.getError().key());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateRoles should replace the roles and persist")
    void testUpdateRoles() {
        var id = UUID.randomUUID();
        var existing = Application.builder().id(id).build();
        when(applicationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var roles = List.of(
            new ApplicationRoleRecord("admin", "Grants full administrative access"),
            new ApplicationRoleRecord("user", null));
        var roleDTOs = List.of(
            new ApplicationRoleDTO("admin", "Grants full administrative access"),
            new ApplicationRoleDTO("user", null));
        when(mapper.toRoleDTO(roles.get(0))).thenReturn(roleDTOs.get(0));
        when(mapper.toRoleDTO(roles.get(1))).thenReturn(roleDTOs.get(1));

        var result = service.updateRoles(userPrincipal, id, roles);

        assertEquals(roleDTOs, result.getRoles());
        assertEquals(userPrincipal.getId(), result.getUpdatedBy());
    }

    @Test
    @DisplayName("deleteById should delete an existing application")
    void testDeleteById() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(id)).thenReturn(true);

        service.deleteById(userPrincipal, id);

        var captor = ArgumentCaptor.forClass(UUID.class);
        verify(applicationRepository).deleteById(captor.capture());
        assertEquals(id, captor.getValue());
    }

    @Test
    @DisplayName("deleteById should throw when the application does not exist")
    void testDeleteById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(id)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.deleteById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
        verify(applicationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("regeneratePolicy should generate the script, store checksum and reset the deployment date")
    void testRegeneratePolicy() {
        var id = UUID.randomUUID();
        var application = Application.builder().id(id).code("payroll").build();
        List<ApplicationRule> activeRules = List.of(ApplicationRule.builder().code("R1").priority(1).build());
        when(applicationRepository.findById(id)).thenReturn(Optional.of(application));
        when(applicationRuleRepository.findByApplicationIdAndDisabledFalseOrderByPriorityAsc(id))
            .thenReturn(activeRules);
        when(opaService.generate(application, activeRules)).thenReturn("rendered-policy");
        when(checksumService.compute("rendered-policy")).thenReturn("policy-checksum");
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.regeneratePolicy(id);

        var captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        var saved = captor.getValue();

        assertEquals("rendered-policy", saved.getScript());
        assertEquals("policy-checksum", saved.getScriptChecksum());
        // regeneration must reset the deployment status so the scheduler redeploys the application.
        assertNull(saved.getDeployedAt());
    }

    @Test
    @DisplayName("regeneratePolicy should not update the application when the generated script is unchanged")
    void testRegeneratePolicy_shouldSkipWhenScriptUnchanged() {
        var id = UUID.randomUUID();
        var application = Application.builder().id(id).code("payroll").scriptChecksum("same-checksum").build();
        List<ApplicationRule> activeRules = List.of(ApplicationRule.builder().code("R1").priority(1).build());
        when(applicationRepository.findById(id)).thenReturn(Optional.of(application));
        when(applicationRuleRepository.findByApplicationIdAndDisabledFalseOrderByPriorityAsc(id))
            .thenReturn(activeRules);
        when(opaService.generate(application, activeRules)).thenReturn("rendered-policy");
        when(checksumService.compute("rendered-policy")).thenReturn("same-checksum");

        service.regeneratePolicy(id);

        // The checksum is unchanged: the application must not be saved nor its deployment status reset.
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("regeneratePolicy should throw when the application does not exist")
    void testRegeneratePolicy_shouldThrowWhenApplicationAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.regeneratePolicy(id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
        verify(applicationRepository, never()).save(any());
    }
}
