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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: OpaApplicationDeployer")
class OpaApplicationDeployerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private OpaService opaService;

    @InjectMocks
    private OpaApplicationDeployer deployer;

    @Test
    @DisplayName("deploy should publish the policy, record the deployment date and persist the application")
    void testDeploy() {
        var application = Application.builder().id(UUID.randomUUID()).code("payroll").script("policy").build();
        var deployedAt = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        when(opaService.publish(application)).thenReturn(deployedAt);

        deployer.deploy(application);

        verify(opaService).publish(application);
        var captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        assertEquals(deployedAt, captor.getValue().getDeployedAt());
    }

    @Test
    @DisplayName("deploy should not persist the application when publishing fails")
    void testDeploy_shouldNotSaveOnPublishFailure() {
        var application = Application.builder().id(UUID.randomUUID()).code("payroll").script("policy").build();
        when(opaService.publish(application)).thenThrow(new ApiException(502,
            I18nMessage.of("error.opa.policy.publish_failed")));

        assertThrows(ApiException.class, () -> deployer.deploy(application));

        // A publishing failure must propagate and roll back this application's own transaction (no save).
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("deploy should propagate the exception when persisting the deployment date fails")
    void testDeploy_shouldPropagateOnSaveFailure() {
        var application = Application.builder().id(UUID.randomUUID()).code("payroll").script("policy").build();
        var deployedAt = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        when(opaService.publish(application)).thenReturn(deployedAt);
        when(applicationRepository.save(any())).thenThrow(new RuntimeException("DB failure"));

        // The policy is already published to OPA; the exception must propagate so the transaction
        // rolls back and deployedAt remains null — the scheduler will retry on the next tick.
        assertThrows(RuntimeException.class, () -> deployer.deploy(application));
    }
}
