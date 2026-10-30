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

package io.github.linagora.linid.im.sync.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.ApplicationRole;
import io.github.linagora.linid.im.sync.persistence.model.ComputedRight;
import io.github.linagora.linid.im.sync.persistence.model.PairChecksums;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ApplicationRoleRepository;
import io.github.linagora.linid.im.sync.persistence.repository.ComputedRightRepository;
import io.github.linagora.linid.im.sync.service.RightsComputationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.test.MetaDataInstanceFactory;

@DisplayName("Test class: ComputeRightsProcessor")
class ComputeRightsProcessorTest {

    private static final UUID TECHNICAL_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-00000000a012");

    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-0000000000ff");

    private static final UUID JDOE = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final UUID ASMITH = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    private static final UUID OTHER_APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");

    private static final String INSTANCE_ID = "sync-api-1";

    private static final long JOB_EXECUTION_ID = 42L;

    private static final Set<String> ROLE_NAMES = Set.of("Administrator", "Reader");

    private static final ComputedRightRecord RIGHTS =
        new ComputedRightRecord(true, List.of("Administrator"), "{\"sub\":\"jdoe\"}");

    private final RightsComputationService rightsComputationService = mock(RightsComputationService.class);

    private final ApplicationRoleRepository applicationRoleRepository = mock(ApplicationRoleRepository.class);

    private final AccountComputedRightViewRepository viewRepository = mock(AccountComputedRightViewRepository.class);

    private final ComputedRightRepository computedRightRepository = mock(ComputedRightRepository.class);

    @BeforeEach
    void setUp() {
        StepSynchronizationManager.register(MetaDataInstanceFactory.createStepExecution(
            MetaDataInstanceFactory.createJobExecution(JOB_EXECUTION_ID), "computeRightsStep", 7L));
        when(applicationRoleRepository.findAllByAppId(APPLICATION_ID))
            .thenReturn(List.of(role(APPLICATION_ID, "Administrator"), role(APPLICATION_ID, "Reader")));
        when(rightsComputationService.compute(any(), any())).thenReturn(RIGHTS);
        when(viewRepository.findChecksumsByActIdAndAppId(any(), any()))
            .thenAnswer(invocation -> Optional.of(current(invocation.getArgument(0), invocation.getArgument(1))));
        when(computedRightRepository.findByActIdAndAppId(any(), any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        StepSynchronizationManager.close();
    }

    @Test
    @DisplayName("process should give a new row for a pair never computed, with its rights, the checksums read with "
        + "the pair, the technical account as author, the instance and the job execution")
    void testProcess_newPair() {
        var pair = pair(JDOE, APPLICATION_ID);

        assertEquals(row(TECHNICAL_ACCOUNT_ID), processor().process(pair));
        verify(rightsComputationService).compute(pair, ROLE_NAMES);
    }

    @Test
    @DisplayName("process should update the current row of a pair already computed, with the technical account as "
        + "updater, keeping its identifier and its author")
    void testProcess_existingPair() {
        var current = previousRow();
        when(computedRightRepository.findByActIdAndAppId(JDOE, APPLICATION_ID)).thenReturn(Optional.of(current));

        var row = processor().process(pair(JDOE, APPLICATION_ID));

        assertSame(current, row);
        var expected = row(OTHER_ACCOUNT_ID);
        expected.setCprId(current.getCprId());
        assertEquals(expected, row);
    }

    @ParameterizedTest
    @MethodSource("changedChecksums")
    @DisplayName("process should filter out a pair whose payload, script or claims template checksum changed since it "
        + "was read, without reading its row")
    void testProcess_changedChecksum(final PairChecksums changed) {
        when(viewRepository.findChecksumsByActIdAndAppId(JDOE, APPLICATION_ID)).thenReturn(Optional.of(changed));

        assertNull(processor().process(pair(JDOE, APPLICATION_ID)));
        verifyNoInteractions(computedRightRepository);
    }

    @Test
    @DisplayName("process should filter out a pair no longer in the view, its application being no longer deployed")
    void testProcess_pairLeftView() {
        when(viewRepository.findChecksumsByActIdAndAppId(JDOE, APPLICATION_ID)).thenReturn(Optional.empty());

        assertNull(processor().process(pair(JDOE, APPLICATION_ID)));
        verifyNoInteractions(computedRightRepository);
    }

    @Test
    @DisplayName("process should let the RightsComputationException of a pair in error through, for the step to skip "
        + "it, without checking or reading its row")
    void testProcess_pairInError() {
        var exception = new RightsComputationException("Unable to compute the rights of the pair",
            new IllegalStateException("Unclosed tag"));
        when(rightsComputationService.compute(any(), any())).thenThrow(exception);

        var processor = processor();
        assertSame(exception, assertThrows(RightsComputationException.class,
            () -> processor.process(pair(JDOE, APPLICATION_ID))));
        verifyNoInteractions(viewRepository, computedRightRepository);
    }

    @Test
    @DisplayName("process should read the role names of an application at its first pair, and keep them until the end "
        + "of the step")
    void testProcess_roleNames() {
        when(applicationRoleRepository.findAllByAppId(OTHER_APPLICATION_ID)).thenReturn(List.of());
        var processor = processor();

        processor.process(pair(JDOE, APPLICATION_ID));
        processor.process(pair(ASMITH, APPLICATION_ID));
        processor.process(pair(JDOE, OTHER_APPLICATION_ID));

        verify(applicationRoleRepository).findAllByAppId(APPLICATION_ID);
        verify(applicationRoleRepository).findAllByAppId(OTHER_APPLICATION_ID);
        verify(rightsComputationService).compute(pair(ASMITH, APPLICATION_ID), ROLE_NAMES);
        verify(rightsComputationService).compute(pair(JDOE, OTHER_APPLICATION_ID), Set.of());
    }

    @Test
    @DisplayName("process should read the role names again in another step, each step having its own processor")
    void testProcess_roleNamesOfAnotherStep() {
        processor().process(pair(JDOE, APPLICATION_ID));
        processor().process(pair(JDOE, APPLICATION_ID));

        verify(applicationRoleRepository, times(2)).findAllByAppId(APPLICATION_ID);
    }

    private ComputeRightsProcessor processor() {
        return new ComputeRightsProcessor(rightsComputationService, applicationRoleRepository, viewRepository,
            computedRightRepository, TECHNICAL_ACCOUNT_ID, INSTANCE_ID);
    }

    private static Stream<PairChecksums> changedChecksums() {
        return Stream.of(
            new PairChecksums(JDOE, APPLICATION_ID, "new-payload-checksum", "script-checksum",
                "claims-template-checksum"),
            new PairChecksums(JDOE, APPLICATION_ID, "payload-checksum", "new-script-checksum",
                "claims-template-checksum"),
            new PairChecksums(JDOE, APPLICATION_ID, "payload-checksum", "script-checksum",
                "new-claims-template-checksum"));
    }

    private static AccountComputedRightView pair(final UUID actId, final UUID appId) {
        return new AccountComputedRightView(actId, "jdoe", appId, "APP", "{}", Map.of("sub", "jdoe"),
            "payload-checksum", "script-checksum", "claims-template-checksum", true);
    }

    /**
     * Gives the current checksums of a pair, unchanged since it was read.
     */
    private static PairChecksums current(final UUID actId, final UUID appId) {
        return new PairChecksums(actId, appId, "payload-checksum", "script-checksum", "claims-template-checksum");
    }

    private static ApplicationRole role(final UUID appId, final String name) {
        return new ApplicationRole(UUID.randomUUID(), appId, name);
    }

    /**
     * Gives the row expected for the pair of jdoe on the application once its rights are computed.
     */
    private static ComputedRight row(final UUID createdBy) {
        var row = new ComputedRight();
        row.setActId(JDOE);
        row.setAppId(APPLICATION_ID);
        row.setAllow(true);
        row.setRoles(List.of("Administrator"));
        row.setClaims("{\"sub\":\"jdoe\"}");
        row.setPayloadChecksum("payload-checksum");
        row.setScriptChecksum("script-checksum");
        row.setClaimsTemplateChecksum("claims-template-checksum");
        row.setSyncInstanceId(INSTANCE_ID);
        row.setSyncJobExecutionId(JOB_EXECUTION_ID);
        row.setCreatedBy(createdBy);
        row.setUpdatedBy(TECHNICAL_ACCOUNT_ID);
        return row;
    }

    /**
     * Gives the row of the pair of jdoe on the application written by a previous computation.
     */
    private static ComputedRight previousRow() {
        var row = new ComputedRight();
        row.setCprId(UUID.randomUUID());
        row.setActId(JDOE);
        row.setAppId(APPLICATION_ID);
        row.setAllow(false);
        row.setRoles(List.of());
        row.setClaims("{\"sub\":\"previous\"}");
        row.setPayloadChecksum("previous-payload-checksum");
        row.setScriptChecksum("previous-script-checksum");
        row.setClaimsTemplateChecksum("previous-claims-template-checksum");
        row.setSyncInstanceId("sync-api-0");
        row.setSyncJobExecutionId(7L);
        row.setCreatedBy(OTHER_ACCOUNT_ID);
        row.setUpdatedBy(OTHER_ACCOUNT_ID);
        return row;
    }
}
