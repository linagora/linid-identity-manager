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

import io.github.linagora.linid.im.api.model.superset.SupersetRlsConfig;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.AccountDistinctView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitDistinctView;
import io.github.linagora.linid.im.api.persistence.repository.AccountDistinctViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitDistinctViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SupersetServiceImpl")
class SupersetServiceImplTest {

    @Mock
    private AccountDistinctViewRepository accountDistinctViewRepository;

    @Mock
    private OrganizationalUnitDistinctViewRepository organizationalUnitDistinctViewRepository;

    @Mock
    private SupersetCacheService supersetCacheService;

    @Mock
    private JinjaService jinjaService;

    private SupersetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupersetServiceImpl(
            "http://localhost:8088",
            "",
            supersetCacheService,
            jinjaService,
            accountDistinctViewRepository,
            organizationalUnitDistinctViewRepository
        );
    }

    @Test
    @DisplayName("should return no RLS rules when RLS configuration is empty")
    void shouldReturnNoRlsRulesWhenConfigurationIsEmpty() {
        var tokenRecord = new SupersetTokenRecord(
            "unknown-dashboard",
            UUID.randomUUID(),
            UUID.randomUUID().toString()
        );

        var userPrincipal = mock(UserPrincipal.class);

        var result = service.buildRlsRules(userPrincipal, tokenRecord);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return only enabled RLS rules matching the dashboard slug")
    void shouldReturnOnlyMatchingEnabledRlsRules() {
        var dashboardId = UUID.randomUUID();
        var rlsId = UUID.randomUUID().toString();
        var clause = "rls_id='{{ entity.externalId | replace(\"'\", \"''\") }}'";

        var matchingConfig = new SupersetRlsConfig(
            "dashboard",
            true,
            10,
            "ACCOUNT",
            clause
        );

        var disabledConfig = new SupersetRlsConfig(
            "dashboard",
            false,
            20,
            "ACCOUNT",
            clause
        );

        var otherDashboardConfig = new SupersetRlsConfig(
            "other-dashboard",
            true,
            30,
            "ACCOUNT",
            clause
        );

        var account = AccountDistinctView.builder().externalId("john.doe").build();

        when(accountDistinctViewRepository.findFirstById(UUID.fromString(rlsId)))
            .thenReturn(Optional.of(account));

        when(jinjaService.render(clause, Map.of("entity", account)))
            .thenReturn("rls_id='john.doe'");

        setRlsConfigurations(
            matchingConfig,
            disabledConfig,
            otherDashboardConfig
        );

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            dashboardId,
            rlsId
        );

        var userPrincipal = mock(UserPrincipal.class);

        var result = service.buildRlsRules(userPrincipal, tokenRecord);

        assertThat(result)
            .hasSize(1)
            .first()
            .satisfies(rule -> {
                assertThat(rule.clause()).isEqualTo("rls_id='john.doe'");
                assertThat(rule.dataset()).isEqualTo(10);
            });
    }

    @Test
    @DisplayName("should build an account RLS rule from the configuration")
    void shouldBuildAccountRlsRule() {
        var rlsId = UUID.randomUUID();
        var clause = "rls_id='{{ entity.externalId | replace(\"'\", \"''\") }}'";
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "ACCOUNT",
            clause
        );

        var account = AccountDistinctView.builder().externalId("john.doe").build();

        when(accountDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.of(account));

        when(jinjaService.render(clause, Map.of("entity", account)))
            .thenReturn("rls_id='john.doe'");

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            rlsId.toString()
        );

        var result = service.buildRlsRule(
            mock(UserPrincipal.class),
            tokenRecord,
            config
        );

        assertThat(result.clause()).isEqualTo("rls_id='john.doe'");
        assertThat(result.dataset()).isEqualTo(42);
    }

    @Test
    @DisplayName("should build an organizational unit RLS rule from the configuration")
    void shouldBuildOrganizationalUnitRlsRule() {
        var rlsId = UUID.randomUUID();
        var clause = "rls_id='{{ entity.name | replace(\"'\", \"''\") }}'";
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "ORGANIZATIONAL_UNIT",
            clause
        );

        var organizationalUnit = OrganizationalUnitDistinctView.builder().name("IT").build();

        when(organizationalUnitDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.of(organizationalUnit));

        when(jinjaService.render(clause, Map.of("entity", organizationalUnit)))
            .thenReturn("rls_id='IT'");

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            rlsId.toString()
        );

        var result = service.buildRlsRule(
            mock(UserPrincipal.class),
            tokenRecord,
            config
        );

        assertThat(result.clause()).isEqualTo("rls_id='IT'");
        assertThat(result.dataset()).isEqualTo(42);
    }

    @Test
    @DisplayName("should reject an unsupported RLS entity")
    void shouldRejectUnsupportedRlsEntity() {
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "UNKNOWN_ENTITY",
            "rls_id='{{ entity.name | replace(\"'\", \"''\") }}'"
        );

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            UUID.randomUUID().toString()
        );

        assertThatThrownBy(() ->
            service.buildRlsRule(
                mock(UserPrincipal.class),
                tokenRecord,
                config
            )
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should throw an exception when the RLS configuration file cannot be read")
    void shouldThrowWhenRlsConfigurationFileCannotBeRead(@TempDir final Path tempDir) {
        Path missingFile = tempDir.resolve("missing.yml");

        assertThatThrownBy(() ->
            service.initRlsConfiguration(missingFile.toString())
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should leave RLS configuration empty when the configuration path is blank")
    void shouldLeaveConfigurationEmptyWhenPathIsBlank() {
        service.initRlsConfiguration(" ");

        var result = service.buildRlsRules(
            mock(UserPrincipal.class),
            new SupersetTokenRecord(
                "dashboard",
                UUID.randomUUID(),
                UUID.randomUUID().toString()
            )
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return the account when the account exists")
    void shouldReturnAccountWhenAccountExists() {
        var rlsId = UUID.randomUUID();
        var account = AccountDistinctView.builder().externalId("john.doe").build();

        when(accountDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.of(account));

        var result = service.getAccount(rlsId.toString());

        assertThat(result).isEqualTo(account);
    }

    @Test
    @DisplayName("should throw a not found exception when the account does not exist")
    void shouldThrowWhenAccountDoesNotExist() {
        var rlsId = UUID.randomUUID();

        when(accountDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.getAccount(rlsId.toString())
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should throw a not found exception when the account rlsId is blank")
    void shouldThrowWhenAccountRlsIdIsBlank() {
        assertThatThrownBy(() ->
            service.getAccount(" ")
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should return the organizational unit when the organizational unit exists")
    void shouldReturnOrganizationalUnitWhenOrganizationalUnitExists() {
        var rlsId = UUID.randomUUID();
        var organizationalUnit = OrganizationalUnitDistinctView.builder().name("IT").build();

        when(organizationalUnitDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.of(organizationalUnit));

        var result = service.getOrganizationalUnit(rlsId.toString());

        assertThat(result).isEqualTo(organizationalUnit);
    }

    @Test
    @DisplayName("should throw a not found exception when the organizational unit does not exist")
    void shouldThrowWhenOrganizationalUnitDoesNotExist() {
        var rlsId = UUID.randomUUID();

        when(organizationalUnitDistinctViewRepository.findFirstById(rlsId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.getOrganizationalUnit(rlsId.toString())
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should throw a not found exception when the organizational unit rlsId is blank")
    void shouldThrowWhenOrganizationalUnitRlsIdIsBlank() {
        assertThatThrownBy(() ->
            service.getOrganizationalUnit(" ")
        )
            .isInstanceOf(ApiException.class);
    }

    private void setRlsConfigurations(final SupersetRlsConfig... configs) {
        service.initRlsConfiguration("");
        try {
            var field = SupersetServiceImpl.class.getDeclaredField("rlsConfigs");
            field.setAccessible(true);
            field.set(service, List.of(configs));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to configure RLS configurations for test", e);
        }
    }
}
