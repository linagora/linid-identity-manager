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

import io.github.linagora.linid.im.api.model.application.ApplicationRecord;
import io.github.linagora.linid.im.api.model.common.AvatarEntity;
import io.github.linagora.linid.im.api.model.sync.RecomputeRightsDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.ApplicationService;
import io.github.linagora.linid.im.api.service.AvatarService;
import io.github.linagora.linid.im.api.service.OpaApplicationDeployerService;
import io.github.linagora.linid.im.api.service.SyncApiService;
import io.github.linagora.linid.im.api.model.application.ApplicationMapper;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ApplicationController")
class ApplicationControllerTest {

    @Mock
    private AvatarService avatarService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private PagedResponseStatusResolver pagedResponseStatusResolver;

    @Mock
    private OpaApplicationDeployerService opaApplicationDeployerService;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private SyncApiService syncApiService;

    private ApplicationController controller;

    private UserPrincipal userPrincipal;

    private ApplicationRecord record;

    @BeforeEach
    void setUp() {
        controller = new ApplicationController(
            avatarService,
            applicationService,
            applicationMapper,
            pagedResponseStatusResolver,
            opaApplicationDeployerService,
            syncApiService
        );
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        record = new ApplicationRecord("my-app", "My Application", null, "OIDC", "{}", "Security", Map.of());
    }

    @Test
    @DisplayName("Should create application")
    void testCreate() {
        when(applicationService.create(any(), any())).thenReturn(new Application());

        var response = controller.create(userPrincipal, record);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("Should find applications")
    void testFindAll() {
        when(applicationService.findAll(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(pagedResponseStatusResolver.resolve(any())).thenReturn(ResponseEntity.ok(new PageImpl<>(List.of())));

        var response = controller.findAll(userPrincipal, new ApplicationViewQueryFilterDto(), Pageable.unpaged());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should find application by id")
    void testFindById() {
        when(applicationService.findViewById(any(), any())).thenReturn(new ApplicationView());

        var response = controller.findById(userPrincipal, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should update application")
    void testUpdate() {
        when(applicationService.update(any(), any(), any())).thenReturn(new Application());

        var response = controller.update(userPrincipal, UUID.randomUUID(), record);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should delete application by id")
    void testDeleteById() {
        var id = UUID.randomUUID();
        doNothing().when(applicationService).deleteById(any(), any());

        var response = controller.deleteById(userPrincipal, id);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(avatarService).deleteQuietly(AvatarEntity.APPLICATIONS, id);
    }

    @Test
    @DisplayName("Should deploy application with force=false")
    void testDeploy_withForceFalse() {
        var id = UUID.randomUUID();
        var deployedApplication = new Application();

        when(opaApplicationDeployerService.deploy(userPrincipal, id, false)).thenReturn(deployedApplication);

        var response = controller.deploy(userPrincipal, id, false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(opaApplicationDeployerService).deploy(userPrincipal, id, false);
    }

    @Test
    @DisplayName("Should deploy application with force=true")
    void testDeploy_withForceTrue() {
        var id = UUID.randomUUID();
        var deployedApplication = new Application();

        when(opaApplicationDeployerService.deploy(userPrincipal, id, true)).thenReturn(deployedApplication);

        var response = controller.deploy(userPrincipal, id, true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(opaApplicationDeployerService).deploy(userPrincipal, id, true);
    }

    @Test
    @DisplayName("recomputeRights should check that the application is deployed, then answer 202 with the answer of "
        + "sync-api")
    void testRecomputeRights_shouldReturn202WithTheAnswerOfSyncApi() {
        var id = UUID.randomUUID();
        var answer = new RecomputeRightsDTO(true);
        when(applicationService.findViewById(userPrincipal, id))
            .thenReturn(applicationView(true, OffsetDateTime.now()));
        when(syncApiService.recomputeApplicationRights(id)).thenReturn(answer);

        var response = controller.recomputeRights(userPrincipal, id);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(answer, response.getBody());
    }

    @Test
    @DisplayName("recomputeRights should propagate the 404 of an unknown application without calling sync-api")
    void testRecomputeRights_shouldPropagateUnknownApplication() {
        var id = UUID.randomUUID();
        when(applicationService.findViewById(userPrincipal, id))
            .thenThrow(new ApiException(404, I18nMessage.of("error.application.not_found")));

        var exception = assertThrows(ApiException.class, () -> controller.recomputeRights(userPrincipal, id));

        assertEquals(404, exception.getStatusCode());
        verifyNoInteractions(syncApiService);
    }

    @Test
    @DisplayName("recomputeRights should answer 400 without calling sync-api when the application has no script")
    void testRecomputeRights_whenScriptIsMissing() {
        assertNotDeployed(applicationView(false, OffsetDateTime.now()));
    }

    @Test
    @DisplayName("recomputeRights should answer 400 without calling sync-api when deployedAt is null")
    void testRecomputeRights_whenNotDeployed() {
        assertNotDeployed(applicationView(true, null));
    }

    @Test
    @DisplayName("Should export application Rego script")
    void testExportScript() {
        var id = UUID.randomUUID();
        var script = "package application\n\nallow := true";

        var applicationView = new ApplicationView();
        applicationView.setCode("my-app");
        applicationView.setScript(script);

        when(applicationService.findViewById(userPrincipal, id)).thenReturn(applicationView);

        var response = controller.exportScript(userPrincipal, id);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertEquals(
            "attachment; filename=\"my-app.rego\"",
            response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertEquals(
            script,
            new String(response.getBody(), StandardCharsets.UTF_8)
        );

        verify(applicationService).findViewById(userPrincipal, id);
    }

    @Test
    @DisplayName("Should fail exporting application Rego script when script is missing")
    void testExportScript_whenScriptIsMissing() {
        var id = UUID.randomUUID();

        var applicationView = new ApplicationView();
        applicationView.setCode("my-app");
        applicationView.setScript(null);

        when(applicationService.findViewById(userPrincipal, id)).thenReturn(applicationView);

        var exception = assertThrows(
            ApiException.class,
            () -> controller.exportScript(userPrincipal, id)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode());

        verify(applicationService).findViewById(userPrincipal, id);
    }

    @Test
    @DisplayName("Should fail exporting application Rego script when script is blank")
    void testExportScript_whenScriptIsBlank() {
        var id = UUID.randomUUID();

        var applicationView = new ApplicationView();
        applicationView.setCode("my-app");
        applicationView.setScript("   ");

        when(applicationService.findViewById(userPrincipal, id)).thenReturn(applicationView);

        var exception = assertThrows(
            ApiException.class,
            () -> controller.exportScript(userPrincipal, id)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode());

        verify(applicationService).findViewById(userPrincipal, id);
    }

    private void assertNotDeployed(final ApplicationView application) {
        var id = UUID.randomUUID();
        when(applicationService.findViewById(userPrincipal, id)).thenReturn(application);

        var exception = assertThrows(ApiException.class, () -> controller.recomputeRights(userPrincipal, id));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("error.application.not_deployed", exception.getError().key());
        assertEquals(Map.of("id", id.toString()), exception.getError().context());
        verifyNoInteractions(syncApiService);
    }

    private static ApplicationView applicationView(final boolean hasScript, final OffsetDateTime deployedAt) {
        var application = new ApplicationView();
        application.setHasScript(hasScript);
        application.setDeployedAt(deployedAt);
        return application;
    }
}
