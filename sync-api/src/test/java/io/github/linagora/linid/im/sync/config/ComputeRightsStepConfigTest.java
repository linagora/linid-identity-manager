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

package io.github.linagora.linid.im.sync.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ComputeRightsStepConfig")
class ComputeRightsStepConfigTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    private static final UUID OTHER_APPLICATION_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");

    private static final UUID LOWEST_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final int CHUNK_SIZE = 2;

    private static final Limit LIMIT = Limit.of(CHUNK_SIZE);

    @Mock
    private AccountComputedRightViewRepository repository;

    private ComputeRightsStepConfig config;

    @BeforeEach
    void setUp() {
        config = new ComputeRightsStepConfig(repository, CHUNK_SIZE);
    }

    @Test
    @DisplayName("constructor should reject a chunk size that is not positive")
    void testConstructor_chunkSizeNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new ComputeRightsStepConfig(repository, 0));
    }

    @Test
    @DisplayName("cyclePairsReader should read the pairs by windows, each one starting after the last pair read and "
        + "bounded by its account")
    void testCyclePairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(ACCOUNT_ID, OTHER_APPLICATION_ID);
        var third = pair(OTHER_ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(true, first, second));
        when(repository.findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            ACCOUNT_ID, positionOf(second), LIMIT))
            .thenReturn(window(false, third));

        ItemReader<AccountComputedRightView> reader = config.cyclePairsReader();

        verifyNoInteractions(repository);
        assertEquals(first, reader.read());
        assertEquals(second, reader.read());
        verify(repository, never()).findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
            ACCOUNT_ID, positionOf(second), LIMIT);
        assertEquals(third, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("accountPairsReader should read the pairs of the account of the actId job parameter")
    void testAccountPairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByActIdAndToComputeTrueOrderByAppIdAsc(ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(false, first));

        ItemReader<AccountComputedRightView> reader = config.accountPairsReader(ACCOUNT_ID);

        assertEquals(first, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("applicationPairsReader should read the pairs of the application of the appId job parameter, each "
        + "window bounded by the account of the last pair read")
    void testApplicationPairsReader() throws Exception {
        var first = pair(ACCOUNT_ID, APPLICATION_ID);
        var second = pair(OTHER_ACCOUNT_ID, APPLICATION_ID);
        when(repository.findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
            APPLICATION_ID, LOWEST_ACCOUNT_ID, ScrollPosition.keyset(), LIMIT))
            .thenReturn(window(true, first));
        when(repository.findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
            APPLICATION_ID, ACCOUNT_ID, positionOf(first), LIMIT))
            .thenReturn(window(false, second));

        ItemReader<AccountComputedRightView> reader = config.applicationPairsReader(APPLICATION_ID);

        assertEquals(first, reader.read());
        assertEquals(second, reader.read());
        assertNull(reader.read());
        verifyNoMoreInteractions(repository);
    }

    private static AccountComputedRightView pair(final UUID actId, final UUID appId) {
        var pair = new AccountComputedRightView();
        pair.setActId(actId);
        pair.setAppId(appId);
        pair.setToCompute(true);
        return pair;
    }

    private static Window<AccountComputedRightView> window(final boolean hasNext,
                                                           final AccountComputedRightView... pairs) {
        return Window.from(List.of(pairs), index -> positionOf(pairs[index]), hasNext);
    }

    private static ScrollPosition positionOf(final AccountComputedRightView pair) {
        return ScrollPosition.forward(Map.of("actId", pair.getActId(), "appId", pair.getAppId()));
    }
}
