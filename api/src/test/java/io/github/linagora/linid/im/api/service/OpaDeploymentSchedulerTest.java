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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: OpaDeploymentScheduler")
class OpaDeploymentSchedulerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private OpaApplicationDeployerService opaApplicationDeployerService;

    @InjectMocks
    private OpaDeploymentScheduler scheduler;

    @Test
    @DisplayName("deployPendingApplications should deploy every pending application independently")
    void testDeployPendingApplications() {
        var first = Application.builder().id(UUID.randomUUID()).code("app-a").script("policy-a").build();
        var second = Application.builder().id(UUID.randomUUID()).code("app-b").script("policy-b").build();
        when(applicationRepository.findByDeployedAtIsNullAndScriptIsNotNull()).thenReturn(List.of(first, second));
        when(opaApplicationDeployerService.deploy(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.deployPendingApplications();

        verify(opaApplicationDeployerService).deploy(first);
        verify(opaApplicationDeployerService).deploy(second);
    }

    @Test
    @DisplayName("deployPendingApplications should isolate a single application failure")
    void testDeployPendingApplications_isolatesFailures() {
        var failing = Application.builder().id(UUID.randomUUID()).code("app-fail").script("policy-fail").build();
        var ok = Application.builder().id(UUID.randomUUID()).code("app-ok").script("policy-ok").build();
        when(applicationRepository.findByDeployedAtIsNullAndScriptIsNotNull()).thenReturn(List.of(failing, ok));
        doThrow(new RuntimeException("deploy failed")).when(opaApplicationDeployerService).deploy(failing);
        when(opaApplicationDeployerService.deploy(ok)).thenReturn(ok);

        scheduler.deployPendingApplications();

        // A failure on the first application must not prevent the second one from being deployed.
        verify(opaApplicationDeployerService).deploy(failing);
        verify(opaApplicationDeployerService).deploy(ok);
    }

    @Test
    @DisplayName("deployPendingApplications should do nothing when no application is pending")
    void testDeployPendingApplications_noPending() {
        when(applicationRepository.findByDeployedAtIsNullAndScriptIsNotNull()).thenReturn(List.of());

        scheduler.deployPendingApplications();

        verify(opaApplicationDeployerService, never()).deploy(any());
    }
}
