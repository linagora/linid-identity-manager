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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleMapper;
import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRuleViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ApplicationRuleServiceImpl")
class ApplicationRuleServiceImplTest {

    @Mock
    private ApplicationRuleRepository applicationRuleRepository;

    @Mock
    private ApplicationRuleViewRepository applicationRuleViewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationRuleMapper mapper;

    @Mock
    private ChecksumService checksumService;

    @InjectMocks
    private ApplicationRuleServiceImpl service;

    private UserPrincipal userPrincipal;

    private UUID applicationId;

    private ApplicationRuleRecord record;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        applicationId = UUID.randomUUID();
        record = new ApplicationRuleRecord("RULE_2", "Second rule", 2, "return false;", false);
    }

    @Test
    @DisplayName("create should force disabled, compute the checksum and persist the mapped rule")
    void testCreate() {
        var mapped = ApplicationRule.builder().code("RULE_2").script("return false;").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRuleRepository.existsByApplicationIdAndCode(applicationId, "RULE_2"))
            .thenReturn(false);
        when(mapper.toEntity(record, userPrincipal)).thenReturn(mapped);
        when(checksumService.compute("return false;")).thenReturn("checksum-value");
        when(applicationRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, applicationId, record);

        var captor = ArgumentCaptor.forClass(ApplicationRule.class);
        verify(applicationRuleRepository).save(captor.capture());
        var saved = captor.getValue();

        assertSame(mapped, saved);
        assertSame(mapped, result);
        assertEquals(applicationId, saved.getApplicationId());
        // disabled must always be true on creation.
        assertTrue(saved.getDisabled());
        assertEquals("checksum-value", saved.getScriptChecksum());
    }

    @Test
    @DisplayName("create should throw when the application does not exist")
    void testCreate_shouldThrowWhenApplicationAbsent() {
        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, applicationId, record));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
        verify(applicationRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw when the code already exists")
    void testCreate_shouldThrowOnDuplicateCode() {
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRuleRepository.existsByApplicationIdAndCode(applicationId, "RULE_2"))
            .thenReturn(true);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, applicationId, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_rule.code.already_exists", exception.getError().key());
        verify(applicationRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll should throw when the application does not exist")
    void testFindAll_shouldThrowWhenApplicationAbsent() {
        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.findAll(
            userPrincipal, applicationId, new ApplicationRuleViewQueryFilterDto(), Pageable.unpaged()));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findAll should query the view repository with a specification")
    void testFindAll() {
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRuleViewRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(new ApplicationRuleView())));

        var page = service.findAll(userPrincipal, applicationId,
            new ApplicationRuleViewQueryFilterDto(), Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("findAll should scope results to the requested application")
    void testFindAll_shouldScopeToApplication() {
        // This pins down the mechanism preventing /applications/{A}/rules from leaking rules of
        // application B: the composed specification must embed the requested applicationId, so two
        // different applications must produce two different specifications for the same filters.
        when(applicationRepository.existsById(any())).thenReturn(true);
        when(applicationRuleViewRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        var otherApplicationId = UUID.randomUUID();
        var filters = new ApplicationRuleViewQueryFilterDto();

        service.findAll(userPrincipal, applicationId, filters, Pageable.unpaged());
        service.findAll(userPrincipal, otherApplicationId, filters, Pageable.unpaged());

        var specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(applicationRuleViewRepository, org.mockito.Mockito.times(2))
            .findAll(specCaptor.capture(), any(Pageable.class));

        assertApplicationScopedPredicateBuilt(specCaptor.getAllValues().get(0));
        assertApplicationScopedPredicateBuilt(specCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("findById should throw when the rule does not exist for the application")
    void testFindById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.findById(userPrincipal, applicationId, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_rule.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findViewById should return the scoped view")
    void testFindViewById() {
        var id = UUID.randomUUID();
        var view = new ApplicationRuleView();
        when(applicationRuleViewRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(view));

        var result = service.findViewById(userPrincipal, applicationId, id);

        assertSame(view, result);
    }

    @Test
    @DisplayName("findViewById should throw when the rule does not exist for the application")
    void testFindViewById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRuleViewRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.findViewById(userPrincipal, applicationId, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_rule.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("update should apply record fields, recompute the checksum and persist the same entity")
    void testUpdate_shouldRecomputeChecksumWhenScriptChanges() {
        var id = UUID.randomUUID();
        var existing = ApplicationRule.builder()
            .id(id)
            .applicationId(applicationId)
            .code("RULE_2")
            .priority(1)
            .script("return true;")
            .scriptChecksum("old-checksum")
            .disabled(true)
            .build();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(existing));
        when(checksumService.compute("return false;")).thenReturn("new-checksum");
        when(applicationRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, applicationId, id, record);

        var captor = ArgumentCaptor.forClass(ApplicationRule.class);
        verify(applicationRuleRepository).save(captor.capture());
        var saved = captor.getValue();

        assertSame(existing, saved);
        assertSame(saved, result);
        assertEquals("RULE_2", saved.getCode());
        assertEquals("Second rule", saved.getDescription());
        assertEquals(2, saved.getPriority());
        assertEquals("return false;", saved.getScript());
        assertEquals("new-checksum", saved.getScriptChecksum());
        assertEquals(false, saved.getDisabled());
        assertEquals(userPrincipal.getId(), saved.getUpdatedBy());
    }

    @Test
    @DisplayName("update should not recompute the checksum when the script is unchanged")
    void testUpdate_shouldKeepChecksumWhenScriptUnchanged() {
        var id = UUID.randomUUID();
        var existing = ApplicationRule.builder()
            .id(id)
            .applicationId(applicationId)
            .code("RULE_2")
            .priority(1)
            .script("return false;")
            .scriptChecksum("kept-checksum")
            .disabled(true)
            .build();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(existing));
        when(applicationRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, applicationId, id, record);

        assertEquals("kept-checksum", result.getScriptChecksum());
        verify(checksumService, never()).compute(any());
    }

    @Test
    @DisplayName("update should not check code uniqueness when the code is unchanged")
    void testUpdate_shouldNotCheckCodeWhenUnchanged() {
        var id = UUID.randomUUID();
        var existing = ApplicationRule.builder()
            .id(id)
            .applicationId(applicationId)
            .code("RULE_2")
            .priority(1)
            .script("return false;")
            .scriptChecksum("kept-checksum")
            .disabled(true)
            .build();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(existing));
        when(applicationRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(userPrincipal, applicationId, id, record);

        verify(applicationRuleRepository, never())
            .existsByApplicationIdAndCodeAndIdNot(any(), any(), any());
    }

    @Test
    @DisplayName("update should throw when the rule does not exist for the application")
    void testUpdate_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, applicationId, id, record));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_rule.not_found", exception.getError().key());
        verify(applicationRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw when another rule already uses the new code")
    void testUpdate_shouldThrowOnDuplicateCode() {
        var id = UUID.randomUUID();
        var existing = ApplicationRule.builder()
            .id(id)
            .applicationId(applicationId)
            .code("OLD_CODE")
            .script("return false;")
            .build();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(existing));
        when(applicationRuleRepository.existsByApplicationIdAndCodeAndIdNot(applicationId, "RULE_2", id))
            .thenReturn(true);

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, applicationId, id, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_rule.code.already_exists", exception.getError().key());
        verify(applicationRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById should delete an existing rule of the application")
    void testDeleteById() {
        var id = UUID.randomUUID();
        var existing = ApplicationRule.builder().id(id).applicationId(applicationId).build();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(existing));

        service.deleteById(userPrincipal, applicationId, id);

        var captor = ArgumentCaptor.forClass(ApplicationRule.class);
        verify(applicationRuleRepository).delete(captor.capture());
        assertSame(existing, captor.getValue());
    }

    @Test
    @DisplayName("deleteById should throw when the rule does not exist for the application")
    void testDeleteById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRuleRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.deleteById(userPrincipal, applicationId, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_rule.not_found", exception.getError().key());
        verify(applicationRuleRepository, never()).delete(any(ApplicationRule.class));
    }

    /**
     * Asserts that the given specification, when materialized, reads the {@code applicationId} column,
     * i.e. that the query is restricted to the owning application.
     *
     * <p>The Criteria mocks are lenient because {@code spring-query-filter} performs additional internal
     * calls on the builder we do not need to constrain here; the assertion focuses on the scoping.</p>
     *
     * @param specification the specification to inspect
     */
    @SuppressWarnings("unchecked")
    private void assertApplicationScopedPredicateBuilt(final Specification<ApplicationRuleView> specification) {
        var root = (Root<ApplicationRuleView>) org.mockito.Mockito.mock(Root.class);
        var query = org.mockito.Mockito.mock(CriteriaQuery.class);
        var builder = org.mockito.Mockito.mock(CriteriaBuilder.class, org.mockito.Mockito.RETURNS_MOCKS);
        var path = (Path<Object>) org.mockito.Mockito.mock(Path.class, org.mockito.Mockito.RETURNS_MOCKS);

        org.mockito.Mockito.lenient().when(root.get("applicationId")).thenReturn(path);

        specification.toPredicate(root, query, builder);

        // The composed specification must read the applicationId column: this is what restricts the
        // query to the owning application. The concrete predicate shape is delegated to spring-query-filter.
        verify(root).get("applicationId");
    }
}
